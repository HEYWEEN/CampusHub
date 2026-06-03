package com.campushub.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * agent 模块配置：启用 DeepSeekProperties + 提供调 DeepSeek 用的 RestClient（零新依赖）。
 */
@Configuration
@EnableConfigurationProperties(DeepSeekProperties.class)
public class AgentConfig {

    /** 专用于 DeepSeek 的 RestClient：JDK HttpClient + 读超时，base url / 鉴权头由调用方按需带。 */
    @Bean("deepSeekRestClient")
    public RestClient deepSeekRestClient(DeepSeekProperties props) {
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient());
        factory.setReadTimeout(Duration.ofMillis(props.getTimeoutMs()));
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.getBaseUrl())
                .build();
    }
}
