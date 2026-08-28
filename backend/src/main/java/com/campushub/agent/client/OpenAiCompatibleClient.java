package com.campushub.agent.client;

import com.campushub.agent.config.AiProviderProperties.ActiveProvider;
import com.campushub.agent.exception.AgentUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completions 客户端。支持 DeepSeek、OrcaRouter 等兼容 Provider，
 * 返回 assistant 消息（可能含 tool_calls），并将调用异常统一为 {@link AgentUnavailableException}。
 */
@Component
public class OpenAiCompatibleClient {

    private final RestClient restClient;
    private final ActiveProvider provider;

    public OpenAiCompatibleClient(
            @Qualifier("openAiCompatibleRestClient") RestClient restClient,
            ActiveProvider provider) {
        this.restClient = restClient;
        this.provider = provider;
    }

    /** 调用一次 OpenAI Chat Completions。 */
    public ChatMessage chat(List<ChatMessage> messages, List<ToolDef> tools) {
        if (!provider.isEnabled()) {
            throw unavailable("api-key 未配置");
        }
        try {
            ChatReq req = new ChatReq(provider.model(), messages, tools, "auto", 0.3);
            ChatResp resp = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + provider.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(ChatResp.class);
            if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
                throw unavailable("返回空 choices");
            }
            return resp.choices().get(0).message();
        } catch (RestClientException e) {
            throw new AgentUnavailableException(
                    provider.displayName() + " 调用失败: " + e.getMessage(), e);
        }
    }

    private AgentUnavailableException unavailable(String detail) {
        return new AgentUnavailableException(provider.displayName() + " " + detail);
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
        public static ChatMessage tool(String toolCallId, String content) {
            return new ChatMessage("tool", content, null, toolCallId);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCall(String id, String type, FunctionCall function) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCall(String name, String arguments) {}

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
