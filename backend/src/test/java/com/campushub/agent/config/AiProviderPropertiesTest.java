package com.campushub.agent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderPropertiesTest {

    @Test
    void defaultsToDeepSeekAndKeepsLegacySettings() {
        AiProviderProperties selector = new AiProviderProperties();
        DeepSeekProperties deepSeek = new DeepSeekProperties();
        deepSeek.setApiKey("deepseek-key");

        var active = selector.resolve(deepSeek, new OrcaRouterProperties());

        assertEquals("deepseek", active.id());
        assertEquals("DeepSeek", active.displayName());
        assertEquals("deepseek-key", active.apiKey());
        assertEquals("https://api.deepseek.com", active.baseUrl());
        assertEquals("deepseek-v4-flash", active.model());
        assertTrue(active.isEnabled());
    }

    @Test
    void selectsOrcaRouterCaseInsensitively() {
        AiProviderProperties selector = new AiProviderProperties();
        selector.setProvider(" OrcaRouter ");
        OrcaRouterProperties orcaRouter = new OrcaRouterProperties();
        orcaRouter.setApiKey("orca-key");

        var active = selector.resolve(new DeepSeekProperties(), orcaRouter);

        assertEquals("orcarouter", active.id());
        assertEquals("OrcaRouter", active.displayName());
        assertEquals("orca-key", active.apiKey());
        assertEquals("https://api.orcarouter.ai/v1", active.baseUrl());
        assertEquals("orcarouter/auto", active.model());
        assertTrue(active.isEnabled());
    }

    @Test
    void rejectsUnknownProvider() {
        AiProviderProperties selector = new AiProviderProperties();
        selector.setProvider("unknown");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> selector.resolve(new DeepSeekProperties(), new OrcaRouterProperties()));

        assertTrue(error.getMessage().contains("deepseek / orcarouter"));
    }
}
