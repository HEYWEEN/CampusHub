package com.campushub.auth.controller;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.service.TokenBlacklist;
import com.campushub.common.util.AesUtil;
import com.campushub.common.util.JwtUtil;
import com.campushub.user.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUTH-04 端到端：refresh / logout / 黑名单生效。
 *
 * 关键路径：
 *   - 用 access token 调 refresh → 拒绝
 *   - 用 refresh token 调 refresh → 拿到新 pair，旧 refresh 入黑
 *   - 旧 refresh 再用 → 401
 *   - logout 后 access 立即失效（验证 /api/health/me 401）
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerRefreshLogoutTest {

    @Autowired private WebApplicationContext ctx;
    @Autowired private AuthUserRepository userRepo;
    @Autowired private UserProfileRepository profileRepo;
    @Autowired private AesUtil aes;
    @Autowired private JwtUtil jwt;
    @Autowired private TokenBlacklist blacklist;

    private final ObjectMapper json = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @BeforeEach
    void cleanup() {
        profileRepo.deleteAll();
        userRepo.deleteAll();
        blacklist.resetForTest();
    }

    private AuthUser createUser(String phone) {
        return userRepo.save(new AuthUser(aes.hmacIndex(phone), aes.encrypt(phone)));
    }

    @Test
    void refresh_withAccessToken_returns401() throws Exception {
        AuthUser u = createUser("13800138200");
        String access = jwt.generateAccessToken(u.getId(), "GUEST");

        mockMvc().perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", access))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Refresh")));
    }

    @Test
    void refresh_withValidRefresh_issuesNewPairAndRevokesOld() throws Exception {
        AuthUser u = createUser("13800138201");
        String refresh = jwt.generateRefreshToken(u.getId(), "GUEST");

        MvcResult res = mockMvc().perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        String newAccess = body.path("data").path("accessToken").asText();
        String newRefresh = body.path("data").path("refreshToken").asText();
        assertNotEquals(refresh, newRefresh, "新 refresh 必须与旧的不同");

        // 旧 refresh 再用 → 黑名单拒收
        mockMvc().perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));

        // 新 access 能正常调用受保护接口
        mockMvc().perform(get("/api/health/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(u.getId()));
    }

    @Test
    void logout_revokesAccessImmediately() throws Exception {
        AuthUser u = createUser("13800138202");
        String access = jwt.generateAccessToken(u.getId(), "GUEST");

        // 退出前可访问
        mockMvc().perform(get("/api/health/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());

        // 退出
        mockMvc().perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 退出后该 access 立即失效
        mockMvc().perform(get("/api/health/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("注销")));
    }

    @Test
    void logout_alsoRevokesRefreshIfProvided() throws Exception {
        AuthUser u = createUser("13800138203");
        String access = jwt.generateAccessToken(u.getId(), "GUEST");
        String refresh = jwt.generateRefreshToken(u.getId(), "GUEST");

        mockMvc().perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk());

        // refresh 也被入黑
        mockMvc().perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_requiresLogin_returns401WithoutToken() throws Exception {
        mockMvc().perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));
    }
}
