package com.campushub.integration;

import com.campushub.auth.service.SmsRateLimiter;
import com.campushub.common.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试基类（QA-02 / QA-03 共用，D 提供给全员复用 —— 见 P4 §05「集成测试基类」）。
 *
 * <p>提供：整库清空 + 限流器重置（每个测试方法前）、MockMvc、真实注册登录拿 token、
 * 快速 mint token、JSON 读取辅助。子类（HappyPathIT / CreditExceptionIT / B 的 TaskExceptionIT）
 * 继承后直接写场景，无需重复样板。
 *
 * <p><b>命名约定</b>：当前 CI（{@code mvn clean install} → surefire）只跑 {@code *Test}，
 * 不跑 {@code *IT}。在加 maven-failsafe-plugin 之前，<b>子类请命名为 {@code *Test}</b> 才会在 CI 执行。
 *
 * <p><b>清表说明</b>：收集所有 JpaRepository 逐个 deleteAll。当前测试 schema 无 JPA 级外键约束
 * （跨模块均逻辑外键），删除顺序无关；待 B 引入带 @JoinColumn 的关联后需改为按依赖序或禁用外键。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIT {

    /** MockSmsSender 在 dev/test 固定返回的验证码。 */
    protected static final String FIXED_SMS_CODE = "123456";

    @Autowired protected WebApplicationContext ctx;
    @Autowired protected JwtUtil jwtUtil;
    @Autowired protected SmsRateLimiter smsRateLimiter;
    @Autowired(required = false) protected List<JpaRepository<?, ?>> repositories;

    protected MockMvc mockMvc;
    protected final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void baseSetup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        smsRateLimiter.resetForTest();
        clearAllTables();
    }

    /** 清空所有 JPA 表，保证每个测试方法从干净状态开始。 */
    protected void clearAllTables() {
        if (repositories == null) return;
        for (JpaRepository<?, ?> repo : repositories) {
            repo.deleteAll();
        }
    }

    // ───────── 鉴权辅助 ─────────

    /** 真实走「发码 → 验证码登录」拿到登录态（首次自动建账，verifyStatus=GUEST）。 */
    protected LoginResult registerAndLogin(String phone) throws Exception {
        mockMvc.perform(post("/api/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonOf(Map.of("phone", phone, "scene", "LOGIN_REGISTER"))))
                .andExpect(status().isOk());

        MvcResult res = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonOf(Map.of("phone", phone, "smsCode", FIXED_SMS_CODE))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = bodyOf(res).path("data").path("accessToken").asText();
        long userId = jwtUtil.getUserId(jwtUtil.parse(accessToken));
        return new LoginResult(userId, accessToken);
    }

    /** 直接签发指定用户 + 认证状态的 access token（绕过认证流程，适合只需"已登录主体"的场景）。 */
    protected String mintToken(long userId, String verifyStatus) {
        return jwtUtil.generateAccessToken(userId, verifyStatus);
    }

    /** 默认签发 APPROVED（已实名）token。 */
    protected String mintToken(long userId) {
        return mintToken(userId, "APPROVED");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    // ───────── JSON 辅助 ─────────

    protected String jsonOf(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    /** 把请求体 Map 写成 JSON（允许 null 值，绕过 Map.of 的限制）。 */
    protected String jsonBody(Map<String, Object> fields) throws Exception {
        return json.writeValueAsString(new HashMap<>(fields));
    }

    protected JsonNode bodyOf(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString());
    }

    /** 注册登录结果：用户 id + access token。 */
    protected record LoginResult(long userId, String accessToken) {
    }
}
