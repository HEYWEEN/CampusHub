package com.campushub.integration;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA-03 异常流集成测试（credit 部分，= 看板 CreditExceptionIT）。
 *
 * <p>基于 {@link BaseIT}，走真实注册登录用户 + HTTP 黑盒，覆盖 ≥2 条异常路径：
 * 未登录访问 / 重复提交 / 越权(自评) / 参数非法。仅依赖 A 的 auth，不依赖 B/C，可独立跑绿。
 * 命名为 *Test 以便在当前 CI（surefire）下执行（见 BaseIT 命名约定）。
 */
class CreditExceptionFlowTest extends BaseIT {

    private static final long TASK = 9001L;

    private String reviewBody(long taskId, long revieweeId, int rating) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("taskId", taskId);
        m.put("revieweeId", revieweeId);
        m.put("rating", rating);
        return jsonBody(m);
    }

    /** 异常流 1：未登录访问受保护接口 → 401。 */
    @Test
    void review_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/credit/reviews")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(reviewBody(TASK, 123L, 5)))
                .andExpect(status().isUnauthorized());
    }

    /** 异常流 2：重复提交（同一人对同一任务评两次）→ 409。 */
    @Test
    void review_duplicate_returns409() throws Exception {
        LoginResult publisher = registerAndLogin("13900002001");
        LoginResult acceptor = registerAndLogin("13900002002");
        String body = reviewBody(TASK, acceptor.userId(), 5);

        mockMvc.perform(post("/api/credit/reviews")
                        .header("Authorization", bearer(publisher.accessToken()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/credit/reviews")
                        .header("Authorization", bearer(publisher.accessToken()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10004));
    }

    /** 异常流 3：越权/非法 —— 给自己评价 → 400。 */
    @Test
    void review_self_returns400() throws Exception {
        LoginResult me = registerAndLogin("13900002003");
        mockMvc.perform(post("/api/credit/reviews")
                        .header("Authorization", bearer(me.accessToken()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(reviewBody(TASK, me.userId(), 5)))
                .andExpect(status().isBadRequest());
    }

    /** 异常流 4：参数非法 —— rating 越界 → 400。 */
    @Test
    void review_ratingOutOfRange_returns400() throws Exception {
        LoginResult publisher = registerAndLogin("13900002004");
        LoginResult acceptor = registerAndLogin("13900002005");
        mockMvc.perform(post("/api/credit/reviews")
                        .header("Authorization", bearer(publisher.accessToken()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(reviewBody(TASK, acceptor.userId(), 6)))
                .andExpect(status().isBadRequest());
    }
}
