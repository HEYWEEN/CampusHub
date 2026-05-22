package com.campushub.user.controller;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.util.AesUtil;
import com.campushub.common.util.JwtUtil;
import com.campushub.user.entity.UserAuditLog;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserAuditLogRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * USER-01 ~ USER-04 端到端：
 *   - GET /me 仅本人 + 全字段
 *   - PATCH /me/profile：昵称 happy / 敏感词 400 / 审计写入
 *   - PATCH /me/privacy：变更后 user_audit_log 有记录
 *   - GET /{id}/public：PublicUserVO 不下发 phone/realName/cipher
 *   - GET /me 未登录 401
 */
@SpringBootTest
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired private WebApplicationContext ctx;
    @Autowired private AuthUserRepository userRepo;
    @Autowired private UserProfileRepository profileRepo;
    @Autowired private UserAuditLogRepository auditRepo;
    @Autowired private AesUtil aes;
    @Autowired private JwtUtil jwt;

    private final ObjectMapper json = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @BeforeEach
    void cleanup() {
        auditRepo.deleteAll();
        profileRepo.deleteAll();
        userRepo.deleteAll();
    }

    private AuthUser createUser(String phone, String nickname, VerifyStatus vs) {
        AuthUser u = new AuthUser(aes.hmacIndex(phone), aes.encrypt(phone));
        u.setVerifyStatus(vs);
        u = userRepo.save(u);
        profileRepo.save(new UserProfile(u.getId(), nickname));
        return u;
    }

    private String tokenFor(AuthUser u) {
        return jwt.generateAccessToken(u.getId(), u.getVerifyStatus().name());
    }

    // ─────── USER-04 GET /me ───────

    @Test
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc().perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void getMe_returnsFullFieldsIncludingPhoneMasked() throws Exception {
        AuthUser u = createUser("13800138300", "张三", VerifyStatus.APPROVED);
        String tok = tokenFor(u);

        mockMvc().perform(get("/api/users/me").header("Authorization", "Bearer " + tok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(u.getId()))
                .andExpect(jsonPath("$.data.nickname").value("张三"))
                .andExpect(jsonPath("$.data.verifyStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.phoneMasked").value("138****8300"))
                .andExpect(jsonPath("$.data.hidePublishHistory").value(true))
                .andExpect(jsonPath("$.data.dailyAcceptLimit").value(2));
    }

    // ─────── USER-01 PATCH /me/profile ───────

    @Test
    void updateProfile_happyPath_changesNicknameAndWritesAudit() throws Exception {
        AuthUser u = createUser("13800138301", "张三", VerifyStatus.GUEST);
        String tok = tokenFor(u);

        Map<String, String> body = new HashMap<>();
        body.put("nickname", "李四");
        body.put("avatarUrl", "https://cdn.example.com/a.jpg");

        mockMvc().perform(patch("/api/users/me/profile")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("李四"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/a.jpg"));

        List<UserAuditLog> logs = auditRepo.findTop50ByUserIdOrderByCreatedAtDesc(u.getId());
        assertEquals(2, logs.size(), "应写入 nickname + avatar 两条审计");
        assertTrue(logs.stream().anyMatch(l -> l.getAction() == UserAuditLog.AuditAction.NICKNAME_CHANGE
                && "张三".equals(l.getBeforeValue())
                && "李四".equals(l.getAfterValue())));
    }

    @Test
    void updateProfile_sensitiveWord_returns400AndNoAudit() throws Exception {
        AuthUser u = createUser("13800138302", "张三", VerifyStatus.GUEST);
        String tok = tokenFor(u);

        mockMvc().perform(patch("/api/users/me/profile")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nickname", "傻逼一个"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("敏感词")));

        assertEquals(0, auditRepo.count(), "失败请求不应写审计");
    }

    @Test
    void updateProfile_sameNickname_noAuditWritten() throws Exception {
        AuthUser u = createUser("13800138303", "张三", VerifyStatus.GUEST);
        String tok = tokenFor(u);

        mockMvc().perform(patch("/api/users/me/profile")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nickname", "张三"))))
                .andExpect(status().isOk());

        assertEquals(0, auditRepo.count(), "无变化时不应写审计");
    }

    // ─────── USER-02 PATCH /me/privacy ───────

    @Test
    void updatePrivacy_toggleTwo_writesOneAuditWithBeforeAfter() throws Exception {
        AuthUser u = createUser("13800138304", "张三", VerifyStatus.GUEST);
        String tok = tokenFor(u);

        Map<String, Object> body = new HashMap<>();
        body.put("hidePublishHistory", false);
        body.put("hideAcceptHistory", false);

        mockMvc().perform(patch("/api/users/me/privacy")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidePublishHistory").value(false))
                .andExpect(jsonPath("$.data.hideAcceptHistory").value(false))
                .andExpect(jsonPath("$.data.hideCourseReviews").value(true));

        List<UserAuditLog> logs = auditRepo.findTop50ByUserIdOrderByCreatedAtDesc(u.getId());
        assertEquals(1, logs.size());
        assertEquals(UserAuditLog.AuditAction.PRIVACY_TOGGLE, logs.get(0).getAction());
        assertTrue(logs.get(0).getBeforeValue().contains("publish=true"));
        assertTrue(logs.get(0).getAfterValue().contains("publish=false"));
    }

    @Test
    void updatePrivacy_noChange_noAuditWritten() throws Exception {
        AuthUser u = createUser("13800138305", "张三", VerifyStatus.GUEST);
        String tok = tokenFor(u);

        // 三项默认都是 true，传 true 等于没变
        mockMvc().perform(patch("/api/users/me/privacy")
                        .header("Authorization", "Bearer " + tok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("hidePublishHistory", true))))
                .andExpect(status().isOk());

        assertEquals(0, auditRepo.count());
    }

    // ─────── USER-03 GET /{userId}/public ───────

    @Test
    void publicProfile_returnsPublicUserVoOnly_noPrivacyFields() throws Exception {
        AuthUser u = createUser("13800138306", "张三", VerifyStatus.APPROVED);

        mockMvc().perform(get("/api/users/" + u.getId() + "/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(u.getId()))
                .andExpect(jsonPath("$.data.nickname").value("张三"))
                .andExpect(jsonPath("$.data.verifiedTag").value(true))
                // 隐私字段一律不存在
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.phoneCipher").doesNotExist())
                .andExpect(jsonPath("$.data.phoneHmac").doesNotExist())
                .andExpect(jsonPath("$.data.realName").doesNotExist())
                .andExpect(jsonPath("$.data.studentNo").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void publicProfile_whitelistedNoTokenNeeded() throws Exception {
        AuthUser u = createUser("13800138307", "张三", VerifyStatus.GUEST);

        // 未带 Authorization 也能访问（白名单 /api/users/*/public）
        mockMvc().perform(get("/api/users/" + u.getId() + "/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verifiedTag").value(false));
    }

    @Test
    void publicProfile_nonexistent_returns404() throws Exception {
        mockMvc().perform(get("/api/users/99999999/public"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004));
    }
}
