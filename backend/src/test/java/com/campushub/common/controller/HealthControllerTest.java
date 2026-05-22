package com.campushub.common.controller;

import com.campushub.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证 INF-02 ~ INF-04 端到端：
 *   - /api/health 白名单可匿名访问 + ApiResponse 包装 + traceId 注入
 *   - /api/health/me 无 token → 401 + 业务码 1002
 *   - /api/health/me 带合法 JWT → 返回 userId
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired private WebApplicationContext ctx;
    @Autowired private JwtUtil jwtUtil;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    void healthPing_whitelisted_returnsOk() throws Exception {
        mockMvc().perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void healthMe_withoutToken_returns401() throws Exception {
        mockMvc().perform(get("/api/health/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void healthMe_withValidToken_returnsUserId() throws Exception {
        String token = jwtUtil.generateAccessToken(99L, "APPROVED");

        mockMvc().perform(get("/api/health/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(99))
                .andExpect(jsonPath("$.data.verifyStatus").value("APPROVED"));
    }

    @Test
    void healthMe_withRefreshToken_rejected() throws Exception {
        String refresh = jwtUtil.generateRefreshToken(99L, "APPROVED");
        mockMvc().perform(get("/api/health/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));
    }
}
