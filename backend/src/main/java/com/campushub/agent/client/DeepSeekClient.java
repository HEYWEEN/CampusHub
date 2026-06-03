package com.campushub.agent.client;

import com.campushub.agent.config.DeepSeekProperties;
import com.campushub.agent.exception.AgentUnavailableException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek（OpenAI 兼容）chat completions 客户端。仅暴露一个同步 {@link #chat} 方法，
 * 返回 assistant 消息（可能含 tool_calls）。任何异常归一为 {@link AgentUnavailableException}。
 */
@Component
public class DeepSeekClient {

    private final RestClient restClient;
    private final DeepSeekProperties props;

    public DeepSeekClient(@Qualifier("deepSeekRestClient") RestClient restClient, DeepSeekProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    /**
     * 调一次 chat completions。
     *
     * @param messages 完整对话（含 system / 历史 / 工具结果）
     * @param tools    可用工具定义
     * @return assistant 消息（content 或 toolCalls 二选一）
     */
    public ChatMessage chat(List<ChatMessage> messages, List<ToolDef> tools) {
        if (!props.isEnabled()) {
            throw new AgentUnavailableException("DeepSeek api-key 未配置");
        }
        try {
            ChatReq req = new ChatReq(props.getModel(), messages, tools, "auto", 0.3);
            ChatResp resp = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(ChatResp.class);
            if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
                throw new AgentUnavailableException("DeepSeek 返回空 choices");
            }
            return resp.choices().get(0).message();
        } catch (RestClientException e) {
            throw new AgentUnavailableException("DeepSeek 调用失败: " + e.getMessage(), e);
        }
    }

    // ==================== OpenAI 格式 DTO ====================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatMessage(
            String role,
            String content,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls,
            @JsonProperty("tool_call_id") String toolCallId
    ) {
        public static ChatMessage system(String content) { return new ChatMessage("system", content, null, null); }
        public static ChatMessage user(String content) { return new ChatMessage("user", content, null, null); }
        public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content, null, null); }
        /** tool 执行结果消息（回填给模型）。 */
        public static ChatMessage tool(String toolCallId, String content) {
            return new ChatMessage("tool", content, null, toolCallId);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCall(
            String id,
            String type,
            FunctionCall function
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCall(
            String name,
            /** 参数为 JSON 字符串（OpenAI 规范）。 */
            String arguments
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolDef(String type, FunctionDef function) {
        public static ToolDef fn(String name, String description, Map<String, Object> parameters) {
            return new ToolDef("function", new FunctionDef(name, description, parameters));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunctionDef(String name, String description, Map<String, Object> parameters) {}

    private record ChatReq(
            String model,
            List<ChatMessage> messages,
            List<ToolDef> tools,
            @JsonProperty("tool_choice") String toolChoice,
            double temperature
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResp(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(ChatMessage message, @JsonProperty("finish_reason") String finishReason) {}
}
