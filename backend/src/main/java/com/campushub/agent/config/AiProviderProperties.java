package com.campushub.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/** 选择 AI 助手使用的 OpenAI-compatible Provider。 */
@ConfigurationProperties(prefix = "campushub.ai")
public class AiProviderProperties {

    private String provider = "deepseek";

    public ActiveProvider resolve(DeepSeekProperties deepSeek, OrcaRouterProperties orcaRouter) {
        String selected = provider == null ? "" : provider.strip().toLowerCase(Locale.ROOT);
        return switch (selected) {
            case "deepseek" -> new ActiveProvider(
                    "deepseek", "DeepSeek", deepSeek.getApiKey(), deepSeek.getBaseUrl(),
                    deepSeek.getModel(), deepSeek.getTimeoutMs());
            case "orcarouter" -> new ActiveProvider(
                    "orcarouter", "OrcaRouter", orcaRouter.getApiKey(), orcaRouter.getBaseUrl(),
                    orcaRouter.getModel(), orcaRouter.getTimeoutMs());
            default -> throw new IllegalArgumentException(
                    "不支持的 AI Provider: " + provider + "（可选 deepseek / orcarouter）");
        };
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public record ActiveProvider(
            String id,
            String displayName,
            String apiKey,
            String baseUrl,
            String model,
            int timeoutMs
    ) {
        public boolean isEnabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
