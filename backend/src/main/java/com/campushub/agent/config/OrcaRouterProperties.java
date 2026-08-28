package com.campushub.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** OrcaRouter OpenAI-compatible API 配置。 */
@ConfigurationProperties(prefix = "campushub.orcarouter")
public class OrcaRouterProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.orcarouter.ai/v1";
    private String model = "orcarouter/auto";
    private int timeoutMs = 40000;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
