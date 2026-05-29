package com.campushub.auth.controller;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.repository.SmsCodeRepository;
import com.campushub.auth.service.SmsRateLimiter;
import com.campushub.common.util.AesUtil;
import com.campushub.user.repository.UserProfileRepository;
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
 * AUTH-02 端到端：覆盖三个 token 类接口的核心路径。
 *
 * 共用前置：MockSmsSender 固定返回 123456 → 测试中直接用 "123456" 当验证码。
 * 每个测试前 reset：清 user / sms_code / profile + limiter 状态。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerLoginRegisterTest {

    @Autowired private WebApplicationContext ctx;
    @Autowired private AuthUserRepository userRepo;
    @Autowired private UserProfileRepository profileRepo;
    @Autowired private SmsCodeRepository smsRepo;
    @Autowired private SmsRateLimiter limiter;
    @Autowired private AesUtil aes;

    private final ObjectMapper json = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @BeforeEach
    void cleanup() {
        smsRepo.deleteAll();
        profileRepo.deleteAll();
        userRepo.deleteAll();
        limiter.resetForTest();
    }

    private void sendSms(String phone) throws Exception {
        mockMvc().perform(post("/api/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "scene", "LOGIN_REGISTER"))))
                .andExpect(status().isOk());
    }

    // ────────── /api/auth/token ──────────

    @Test
    void loginByCode_firstTime_createsUserAndProfileWithGuestStatus() throws Exception {
        String phone = "13800138001";
        sendSms(phone);

        mockMvc().perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.verifyStatus").value("guest"));

        // 验证：DB 建账 + profile 同步建好 + 手机号仅 HMAC
        String hmac = aes.hmacIndex(phone);
        Optional<AuthUser> user = userRepo.findByPhoneHmac(hmac);
        assertTrue(user.isPresent());
        assertTrue(profileRepo.findByUserId(user.get().getId()).isPresent());
        assertFalse(user.get().getPhoneHmac().contains(phone), "DB 不应含明文手机号");
        assertNull(user.get().getPasswordHash(), "首次自动建账无密码");
    }

    @Test
    void loginByCode_invalidSms_returns401() throws Exception {
        String phone = "13800138002";
        sendSms(phone);

        mockMvc().perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "999999"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    void loginByCode_reuseConsumedCode_returns401() throws Exception {
        String phone = "13800138003";
        sendSms(phone);

        mockMvc().perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456"))))
                .andExpect(status().isOk());

        // 同一验证码再次使用 — 应被 consumed 拒绝
        mockMvc().perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2001));
    }

    // ────────── /api/auth/register ──────────

    @Test
    void register_setsPasswordAndAllowsPasswordLogin() throws Exception {
        String phone = "13800138004";
        sendSms(phone);

        mockMvc().perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456", "password", "Pass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // 用密码登录验证 hash 已写入
        mockMvc().perform(post("/api/auth/token/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "password", "Pass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void register_alreadySetPassword_returns409() throws Exception {
        String phone = "13800138005";
        sendSms(phone);

        mockMvc().perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456", "password", "Pass1234"))))
                .andExpect(status().isOk());

        // 第二次注册：需要先 reset 限流（60s 内 ≤ 1 次会拦掉 sendSms）
        limiter.resetForTest();
        sendSms(phone);

        mockMvc().perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456", "password", "Other999"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2004));
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        String phone = "13800138006";
        sendSms(phone);

        // 全数字 → 缺字母
        mockMvc().perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456", "password", "12345678"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    // ────────── /api/auth/token/password ──────────

    @Test
    void loginByPassword_userNotExist_returns401NotEnumerable() throws Exception {
        // 故意不发任何验证码、不建任何 user
        mockMvc().perform(post("/api/auth/token/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", "13800138007", "password", "Pass1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("手机号或密码")));
    }

    @Test
    void loginByPassword_wrongPassword_returns401() throws Exception {
        String phone = "13800138008";
        sendSms(phone);
        mockMvc().perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456", "password", "Pass1234"))))
                .andExpect(status().isOk());

        mockMvc().perform(post("/api/auth/token/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "password", "WrongPwd1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    void loginByPassword_userWithoutPasswordSet_returns401WithPasswordNotSet() throws Exception {
        String phone = "13800138009";
        sendSms(phone);
        // 仅做 loginByCode，不设密码
        mockMvc().perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "smsCode", "123456"))))
                .andExpect(status().isOk());

        mockMvc().perform(post("/api/auth/token/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", phone, "password", "Pass1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2005));
    }
}
