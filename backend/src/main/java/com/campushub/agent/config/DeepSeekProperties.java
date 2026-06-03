package com.campushub.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek API 配置（绑定 campushub.deepseek.*）。
 *
 * <pre>
 *   campushub.deepseek.api-key=${DEEPSEEK_API_KEY:}   # 空 → agent 走规则降级
 *   campushub.deepseek.base-url=https://api.deepseek.com
 *   campushub.deepseek.model=deepseek-v4-flash         # function calling ✓
 * </pre>
 */
@ConfigurationProperties(prefix = "campushub.deepseek")
public class DeepSeekProperties {

    /** API Key；为空时 agent 自动降级为规则模式（不调外部 API）。 */
    private String apiKey = "";

    /** OpenAI 兼容 base url。 */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型 id。deepseek-v4-flash 支持工具调用。 */
    private String model = "deepseek-v4-flash";

    /** 读超时（毫秒）—— LLM 响应较慢，给足。 */
    private int timeoutMs = 40000;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
