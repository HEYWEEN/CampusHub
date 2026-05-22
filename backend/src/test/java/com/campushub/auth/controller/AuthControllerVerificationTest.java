package com.campushub.auth.controller;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.VerificationStatus;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.repository.AuthVerificationRepository;
import com.campushub.auth.repository.SmsCodeRepository;
import com.campushub.auth.service.SmsRateLimiter;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.util.AesUtil;
import com.campushub.common.util.JwtUtil;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUTH-03 端到端：
 *   - 未登录 → 401
 *   - 登录后提交 → 200，auth_user.verify_status = PENDING，DB 无明文
 *   - PENDING 中再提交 → 409
 *   - GET /verifications/me 返回最新状态
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerVerificationTest {

    @Autowired private WebApplicationContext ctx;
    @Autowired private AuthUserRepository userRepo;
    @Autowired private UserProfileRepository profileRepo;
    @Autowired private AuthVerificationRepository verRepo;
    @Autowired private SmsCodeRepository smsRepo;
    @Autowired private SmsRateLimiter limiter;
    @Autowired private AesUtil aes;
    @Autowired private JwtUtil jwt;

    private final ObjectMapper json = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @BeforeEach
    void cleanup() {
        verRepo.deleteAll();
        smsRepo.deleteAll();
        profileRepo.deleteAll();
        userRepo.deleteAll();
        limiter.resetForTest();
    }

    private AuthUser createUser(String phone) {
        AuthUser u = new AuthUser(aes.hmacIndex(phone), aes.encrypt(phone));
        return userRepo.save(u);
    }

    private String tokenFor(AuthUser u) {
        return jwt.generateAccessToken(u.getId(), u.getVerifyStatus().name());
    }

    private String fakeImageBase64() {
        return Base64.getEncoder().encodeToString("fake-png-bytes".getBytes(StandardCharsets.UTF_8));
    }

    private String submitBody() throws Exception {
        return json.writeValueAsString(Map.of(
                "realName", "张三",
                "studentNo", "211250000",
                "idCard", "",
                "attachmentsBase64", List.of(fakeImageBase64())
        ));
    }

    @Test
    void submit_withoutToken_returns401() throws Exception {
        mockMvc().perform(post("/api/auth/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void submit_success_updatesUserVerifyStatusAndStoresCipherOnly() throws Exception {
        AuthUser u = createUser("13800138100");
        String tok = tokenFor(u);

        mockMvc().perform(post("/api/auth/verifications")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.attachmentSha256[0]").isString());

        // 用户 verify_status 同步推进到 PENDING
        AuthUser reloaded = userRepo.findById(u.getId()).orElseThrow();
        assertEquals(VerifyStatus.PENDING, reloaded.getVerifyStatus());

        // DB 行不含明文姓名/学号字符
        var ver = verRepo.findTopByUserIdOrderByCreatedAtDesc(u.getId()).orElseThrow();
        assertFalse(ver.getRealNameCipher().contains("张三"));
        assertFalse(ver.getStudentNoCipher().contains("211250000"));
        assertEquals(VerificationStatus.PENDING, ver.getStatus());
    }

    @Test
    void submit_whilePending_returns409() throws Exception {
        AuthUser u = createUser("13800138101");
        String tok = tokenFor(u);

        mockMvc().perform(post("/api/auth/verifications")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isOk());

        // 仍处于 PENDING，再次提交应被拒
        mockMvc().perform(post("/api/auth/verifications")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2004));
    }

    @Test
    void queryMine_afterSubmit_returnsLatest() throws Exception {
        AuthUser u = createUser("13800138102");
        String tok = tokenFor(u);

        mockMvc().perform(post("/api/auth/verifications")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isOk());

        mockMvc().perform(get("/api/auth/verifications/me")
                        .header("Authorization", "Bearer " + tok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.attachmentSha256").isArray());
    }

    @Test
    void queryMine_neverSubmitted_returns404() throws Exception {
        AuthUser u = createUser("13800138103");
        String tok = tokenFor(u);

        mockMvc().perform(get("/api/auth/verifications/me")
                        .header("Authorization", "Bearer " + tok))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004));
    }
}
