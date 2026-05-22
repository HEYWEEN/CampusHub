package com.campushub.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 {@link BaseIT} 基类本身可用（清表 + 注册登录 + token 鉴权链路打通）。
 * 仅依赖 A 已交付的 auth / health，不依赖 B/C，可独立跑绿。
 * 命名为 *Test 以便在当前 CI（surefire）下执行。
 */
class BaseITSmokeTest extends BaseIT {

    @Test
    void registerAndLogin_returnsUsableTokenAndUserId() throws Exception {
        LoginResult login = registerAndLogin("13900001111");

        assertTrue(login.userId() > 0, "应解析出有效 userId");
        // 用真实登录拿到的 token 访问需鉴权接口，应放行
        mockMvc.perform(get("/api/health/me").header("Authorization", bearer(login.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void mintToken_isAcceptedByAuthInterceptor() throws Exception {
        mockMvc.perform(get("/api/health/me").header("Authorization", bearer(mintToken(999L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void noToken_isRejected() throws Exception {
        mockMvc.perform(get("/api/health/me"))
                .andExpect(status().isUnauthorized());
    }
}
