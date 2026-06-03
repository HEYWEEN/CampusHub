package com.campushub.agent.service;

import com.campushub.agent.client.DeepSeekClient;
import com.campushub.agent.client.DeepSeekClient.ChatMessage;
import com.campushub.agent.client.DeepSeekClient.FunctionCall;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentServiceTest {

    @Mock DeepSeekClient deepSeek;
    @Mock ToolExecutor toolExecutor;
    @Mock AgentConversationRepository convRepo;
    @Mock AgentMessageRepository msgRepo;

    AgentService service;
    final ObjectMapper json = new ObjectMapper();

    private static final long USER = 1L;

    @BeforeEach
    void setup() {
        service = new AgentService(deepSeek, toolExecutor, convRepo, msgRepo, json);
        AgentConversation conv = new AgentConversation(USER, "对话");
        ReflectionTestUtils.setField(conv, "id", 7L);
        when(convRepo.findFirstByUserIdOrderByUpdatedAtDesc(USER)).thenReturn(Optional.of(conv));
        when(convRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(msgRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(msgRepo.findByConversationIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .thenReturn(List.of(new AgentMessage(7L, AgentRole.USER, "我想接个取快递的单")));
    }

    private ChatMessage toolCallMsg() {
        ToolCall tc = new ToolCall("call_1", "function",
                new FunctionCall(ToolSpecs.SEARCH_TASKS, "{\"keywords\":[\"取快递\"]}"));
        return new ChatMessage("assistant", null, List.of(tc), null);
    }

    @Test
    void chat_toolCallThenFinalText_collectsActionAndPersists() {
        when(deepSeek.chat(anyList(), anyList()))
                .thenReturn(toolCallMsg(), ChatMessage.assistant("帮你找到了这些单"));
        when(toolExecutor.execute(eq(ToolSpecs.SEARCH_TASKS), any()))
                .thenReturn(new ToolResult("found 2", AgentAction.taskResults(List.of())));

        AgentChatResponse resp = service.chat(USER, "我想接个取快递的单");

        assertEquals("帮你找到了这些单", resp.reply());
        assertEquals(1, resp.actions().size());
        assertEquals("task_results", resp.actions().get(0).type());
        assertEquals(7L, resp.conversationId());
        // user + assistant 各落库一次
        verify(msgRepo, times(2)).save(any(AgentMessage.class));
    }

    @Test
    void chat_noToolCall_returnsPlainText() {
        when(deepSeek.chat(anyList(), anyList())).thenReturn(ChatMessage.assistant("你好呀"));
        AgentChatResponse resp = service.chat(USER, "你好");
        assertEquals("你好呀", resp.reply());
        assertTrue(resp.actions().isEmpty());
        verify(toolExecutor, never()).execute(anyString(), any());
    }

    @Test
    void chat_apiDown_findIntent_fallsBackToRuleSearch() {
        when(deepSeek.chat(anyList(), anyList())).thenThrow(new AgentUnavailableException("no key"));
        when(toolExecutor.execute(eq(ToolSpecs.SEARCH_TASKS), any()))
                .thenReturn(new ToolResult("recent", AgentAction.taskResults(List.of())));

        AgentChatResponse resp = service.chat(USER, "帮我找个取快递的单");

        assertTrue(resp.reply().contains("暂时不可用"));
        assertEquals(1, resp.actions().size()); // 降级也给了搜索结果
    }

    @Test
    void chat_apiDown_postIntent_returnsGuidanceNoSearch() {
        when(deepSeek.chat(anyList(), anyList())).thenThrow(new AgentUnavailableException("no key"));
        AgentChatResponse resp = service.chat(USER, "帮我发布一个跑腿任务");
        assertTrue(resp.reply().contains("发布"));
        assertTrue(resp.actions().isEmpty());
        verify(toolExecutor, never()).execute(anyString(), any());
    }

    @Test
    void history_mapsMessages() {
        when(msgRepo.findByConversationIdOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(
                        new AgentMessage(7L, AgentRole.USER, "hi"),
                        new AgentMessage(7L, AgentRole.ASSISTANT, "hello")));
        var vos = service.history(USER);
        assertEquals(2, vos.size());
        assertEquals("user", vos.get(0).role());
        assertEquals("assistant", vos.get(1).role());
    }
}
