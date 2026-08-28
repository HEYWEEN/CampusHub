package com.campushub.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * agent 模块配置：选择 OpenAI-compatible Provider 并创建对应 RestClient（零新依赖）。
 */
@Configuration
@EnableConfigurationProperties({
        AiProviderProperties.class,
        DeepSeekProperties.class,
        OrcaRouterProperties.class
})
public class AgentConfig {

    @Bean
    public AiProviderProperties.ActiveProvider activeAiProvider(
            AiProviderProperties selector,
            DeepSeekProperties deepSeek,
            OrcaRouterProperties orcaRouter) {
        return selector.resolve(deepSeek, orcaRouter);
    }

    /** AI Provider 专用 RestClient：JDK HttpClient + 读超时，鉴权头由调用方按需带。 */
    @Bean("openAiCompatibleRestClient")
    public RestClient openAiCompatibleRestClient(AiProviderProperties.ActiveProvider provider) {
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient());
        factory.setReadTimeout(Duration.ofMillis(provider.timeoutMs()));
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(provider.baseUrl())
                .build();
    }
}
