package com.campushub.agent.service;

import com.campushub.agent.client.DeepSeekClient;
import com.campushub.agent.client.DeepSeekClient.ChatMessage;
import com.campushub.agent.client.DeepSeekClient.ToolCall;
import com.campushub.agent.entity.AgentConversation;
import com.campushub.agent.entity.AgentMessage;
import com.campushub.agent.entity.AgentRole;
import com.campushub.agent.exception.AgentUnavailableException;
import com.campushub.agent.repository.AgentConversationRepository;
import com.campushub.agent.repository.AgentMessageRepository;
import com.campushub.agent.tool.ToolExecutor;
import com.campushub.agent.tool.ToolResult;
import com.campushub.agent.tool.ToolSpecs;
import com.campushub.agent.vo.AgentAction;
import com.campushub.agent.vo.AgentChatResponse;
import com.campushub.agent.vo.AgentMessageVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 助手核心：工具调用循环 + 多轮历史 + 持久化 + 规则降级。
 *
 * <p>不加类级 @Transactional：LLM 调用较慢，避免长时间占用 DB 连接。各 repo.save 自带短事务。
 */
@Service
public class AgentService {

    private static final int HISTORY_TURNS = 10;
    private static final int MAX_TOOL_ROUNDS = 3;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final DeepSeekClient deepSeek;
    private final ToolExecutor toolExecutor;
    private final AgentConversationRepository convRepo;
    private final AgentMessageRepository msgRepo;
    private final ObjectMapper json;

    public AgentService(DeepSeekClient deepSeek, ToolExecutor toolExecutor,
                        AgentConversationRepository convRepo, AgentMessageRepository msgRepo,
                        ObjectMapper json) {
        this.deepSeek = deepSeek;
        this.toolExecutor = toolExecutor;
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.json = json;
    }

    public AgentChatResponse chat(long userId, String userMessage) {
        AgentConversation conv = convRepo.findFirstByUserIdOrderByUpdatedAtDesc(userId)
                .orElseGet(() -> convRepo.save(new AgentConversation(userId, truncate(userMessage))));
        msgRepo.save(new AgentMessage(conv.getId(), AgentRole.USER, userMessage));

        List<AgentAction> actions = new ArrayList<>();
        String reply;
        try {
            reply = runLlmLoop(conv.getId(), actions);
        } catch (AgentUnavailableException e) {
            reply = ruleFallback(userMessage, actions);
        }

        msgRepo.save(new AgentMessage(conv.getId(), AgentRole.ASSISTANT, reply));
        conv.touch();
        convRepo.save(conv);
        return new AgentChatResponse(conv.getId(), reply, actions);
    }

    public List<AgentMessageVO> history(long userId) {
        return convRepo.findFirstByUserIdOrderByUpdatedAtDesc(userId)
                .map(c -> msgRepo.findByConversationIdOrderByCreatedAtAsc(c.getId()).stream()
                        .map(AgentMessageVO::from).toList())
                .orElseGet(List::of);
    }

    // ==================== LLM 工具循环 ====================

    private String runLlmLoop(long conversationId, List<AgentAction> actions) {
        List<ChatMessage> msgs = new ArrayList<>();
        msgs.add(ChatMessage.system(systemPrompt()));
        for (AgentMessage h : recentHistory(conversationId)) {
            msgs.add(h.getRole() == AgentRole.USER
                    ? ChatMessage.user(h.getContent())
                    : ChatMessage.assistant(h.getContent()));
        }

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatMessage out = deepSeek.chat(msgs, ToolSpecs.all());
            if (out.toolCalls() == null || out.toolCalls().isEmpty()) {
                return nz(out.content());
            }
            msgs.add(out); // 把 assistant 的 tool_calls 加入上下文
            for (ToolCall tc : out.toolCalls()) {
                ToolResult r = toolExecutor.execute(tc.function().name(), parseArgs(tc.function().arguments()));
                if (r.action() != null) actions.add(r.action());
                msgs.add(ChatMessage.tool(tc.id(), r.contentForModel()));
            }
        }
        // 工具轮次用尽 → 不带工具收个尾
        return nz(deepSeek.chat(msgs, List.of()).content());
    }

    private String systemPrompt() {
        String now = ZonedDateTime.now(ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return """
                你是 CampusHub 校园互助平台的 AI 助手，用简洁友好的中文回答。
                能力：① 用户想找单/接单 → 调用 search_tasks；② 用户想发单 → 调用 draft_task 生成草稿（不会自动发布）。
                找单时把口语映射成结构化参数，并在 keywords 里做同义词扩展，让结果更全。
                发单草稿里的 deadlineIso 需根据「当前时间」换算。当前时间：%s。
                没有合适结果时如实说明，不要编造任务。回答尽量短。
                """.formatted(now);
    }

    private List<AgentMessage> recentHistory(long conversationId) {
        List<AgentMessage> desc = msgRepo.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, HISTORY_TURNS));
        List<AgentMessage> asc = new ArrayList<>(desc);
        java.util.Collections.reverse(asc);
        return asc;
    }

    private JsonNode parseArgs(String arguments) {
        try {
            if (arguments == null || arguments.isBlank()) return json.createObjectNode();
            return json.readTree(arguments);
        } catch (Exception e) {
            return json.createObjectNode();
        }
    }

    // ==================== 规则降级（DeepSeek 不可用） ====================

    private String ruleFallback(String userMessage, List<AgentAction> actions) {
        String m = userMessage == null ? "" : userMessage;
        boolean wantPost = m.contains("发布") || m.contains("发个") || m.contains("帮我发");
        boolean wantFind = m.contains("找") || m.contains("搜") || m.contains("接单") || m.contains("接个") || m.contains("有没有");

        if (wantPost && !wantFind) {
            return "AI 助手暂时不可用～你可以直接到「发布任务」页面手动发单。";
        }
        // 默认按「找单」降级：返回最新待接单任务（不依赖 LLM）
        ObjectNode args = json.createObjectNode(); // 空参 → 返回最新候选
        ToolResult r = toolExecutor.execute(ToolSpecs.SEARCH_TASKS, args);
        if (r.action() != null) actions.add(r.action());
        return "AI 助手暂时不可用，先按最新任务为你列几个：";
    }

    private static String truncate(String s) {
        if (s == null) return "新对话";
        String t = s.strip();
        return t.length() <= 40 ? t : t.substring(0, 40);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
