package com.campushub.agent.client;

import com.campushub.agent.config.AiProviderProperties.ActiveProvider;
import com.campushub.agent.exception.AgentUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class OpenAiCompatibleClientTest {

    @Test
    void sendsOrcaRouterChatCompletionAndParsesToolCall() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.orcarouter.ai/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ActiveProvider provider = new ActiveProvider(
                "orcarouter", "OrcaRouter", "orca-key", "https://api.orcarouter.ai/v1",
                "orcarouter/auto", 40000);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(builder.build(), provider);
        OpenAiCompatibleClient.ToolDef tool = OpenAiCompatibleClient.ToolDef.fn(
                "search_tasks", "search", Map.of("type", "object"));

        server.expect(requestTo("https://api.orcarouter.ai/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer orca-key"))
                .andExpect(jsonPath("$.model").value("orcarouter/auto"))
                .andExpect(jsonPath("$.tool_choice").value("auto"))
                .andExpect(jsonPath("$.tools[0].function.name").value("search_tasks"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","tool_calls":[{
                          "id":"call_1","type":"function","function":{"name":"search_tasks","arguments":"{}"}
                        }]},"finish_reason":"tool_calls"}]}
                        """, MediaType.APPLICATION_JSON));

        var response = client.chat(
                List.of(OpenAiCompatibleClient.ChatMessage.user("帮我找任务")), List.of(tool));

        assertEquals("assistant", response.role());
        assertEquals("search_tasks", response.toolCalls().get(0).function().name());
        server.verify();
    }

    @Test
    void missingKeyReportsSelectedProviderWithoutSendingRequest() {
        RestClient restClient = RestClient.builder().baseUrl("https://api.orcarouter.ai/v1").build();
        ActiveProvider provider = new ActiveProvider(
                "orcarouter", "OrcaRouter", "", "https://api.orcarouter.ai/v1",
                "orcarouter/auto", 40000);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restClient, provider);

        AgentUnavailableException error = assertThrows(AgentUnavailableException.class,
                () -> client.chat(List.of(OpenAiCompatibleClient.ChatMessage.user("hi")), List.of()));

        assertTrue(error.getMessage().contains("OrcaRouter api-key 未配置"));
    }

    @Test
    void emptyChoicesReportsSelectedProvider() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.deepseek.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ActiveProvider provider = new ActiveProvider(
                "deepseek", "DeepSeek", "deep-key", "https://api.deepseek.com",
                "deepseek-v4-flash", 40000);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(builder.build(), provider);
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        AgentUnavailableException error = assertThrows(AgentUnavailableException.class,
                () -> client.chat(List.of(OpenAiCompatibleClient.ChatMessage.user("hi")), List.of()));

        assertTrue(error.getMessage().contains("DeepSeek 返回空 choices"));
        server.verify();
    }
}
