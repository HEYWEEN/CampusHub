package com.campushub.auth.controller;

import com.campushub.auth.entity.SmsCode;
import com.campushub.auth.entity.SmsScene;
import com.campushub.auth.repository.SmsCodeRepository;
import com.campushub.auth.service.SmsRateLimiter;
import com.campushub.common.util.AesUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUTH-01 端到端：
 *   - 正常发送 200，DB 有记录且 phone 仅以 HMAC 存储（无明文）
 *   - 手机号格式不合法 400
 *   - 同手机 60s 内重发 429（CODE 1007 RATE_LIMITED）
 *   - 同 IP 60s 内第 6 次 429（IP 档触发）
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerSmsCodeTest {

    @Autowired private WebApplicationContext ctx;
    @Autowired private SmsCodeRepository repo;
    @Autowired private SmsRateLimiter limiter;
    @Autowired private AesUtil aes;

    private final ObjectMapper json = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @BeforeEach
    void cleanup() {
        repo.deleteAll();
        limiter.resetForTest();
    }

    private String body(String phone) throws Exception {
        return json.writeValueAsString(Map.of("phone", phone, "scene", "LOGIN_REGISTER"));
    }

    @Test
    void sendOnce_returnsOkAndPersistsHmacOnly() throws Exception {
        String phone = "13800138000";
        mockMvc().perform(post("/api/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        String hmac = aes.hmacIndex(phone);
        Optional<SmsCode> stored = repo
                .findTopByPhoneHmacAndSceneAndConsumedAtIsNullOrderByCreatedAtDesc(
                        hmac, SmsScene.LOGIN_REGISTER);
        assertTrue(stored.isPresent(), "DB 应有验证码记录");
        assertEquals("123456", stored.get().getCode(), "MockSmsSender 固定 123456");
        // 验证 DB 不含明文手机号字符
        assertFalse(stored.get().getPhoneHmac().contains(phone));
    }

    @Test
    void invalidPhoneFormat_returns400() throws Exception {
        mockMvc().perform(post("/api/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void samePhone_secondCallWithin60s_returns429() throws Exception {
        String phone = "13900139000";
        mockMvc().perform(post("/api/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(phone)))
                .andExpect(status().isOk());

        mockMvc().perform(post("/api/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(phone)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(1007))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("手机号")));
    }

    @Test
    void sameIp_sixCallsWithDifferentPhones_lastReturns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            String phone = "138001380" + String.format("%02d", i);
            mockMvc().perform(post("/api/auth/sms-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(phone)))
                    .andExpect(status().isOk());
        }
        // 第 6 个不同手机号、同一默认 IP（127.0.0.1）→ IP 档触发
        mockMvc().perform(post("/api/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("13800138099")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(1007))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("IP")));
    }
}
