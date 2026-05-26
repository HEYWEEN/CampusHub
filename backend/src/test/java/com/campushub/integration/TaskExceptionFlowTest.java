package com.campushub.integration;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA-03 异常流集成测试（task 部分 = 看板 TaskExceptionIT，v3 由 B 自写）。
 *
 * <p>基于 {@link BaseIT}，走真实注册登录用户 + HTTP 黑盒，覆盖 ≥5 条异常路径：
 * 自接禁止 / 接单上限 / 状态机违法 / 越权编辑 / 越权取消。
 * 命名为 *Test 以便在当前 CI（surefire）下执行（见 BaseIT 命名约定）。
 */
class TaskExceptionFlowTest extends BaseIT {

    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private AuthUserRepository authUserRepo;

    private static final int SEED_BALANCE = 200;

    /** 给用户充积分，让 freeze/unfreeze 有余额可操作。 */
    private void seedBalance(long userId, int points) {
        CreditAccount acc = accountRepo.findByUserId(userId).orElseGet(() -> {
            CreditAccount a = new CreditAccount(userId);
            return accountRepo.save(a);
        });
        acc.deposit(points);
        accountRepo.save(acc);
    }

    /** 将用户设为已认证（task 发布/接单都要求 APPROVED）。 */
    private void approveUser(long userId) {
        AuthUser u = authUserRepo.findById(userId).orElseThrow();
        u.setVerifyStatus(VerifyStatus.APPROVED);
        authUserRepo.save(u);
    }

    /** 注册 + 认证 + 充积分，一步到位。返回登录结果（token 中的 verifyStatus 可能不准但 DB 已更新）。 */
    private LoginResult setupUser(String phone) throws Exception {
        LoginResult user = registerAndLogin(phone);
        approveUser(user.userId());
        seedBalance(user.userId(), SEED_BALANCE);
        return user;
    }

    /** 发布一个跑腿任务，返回 taskId。 */
    private long publishTask(String token) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "帮取快递");
        body.put("taskType", "ERRAND");
        body.put("rewardPoint", 20);
        body.put("deadlineAt", "2026-12-31T23:59:59Z");
        body.put("pickupHint", "韵达2号柜");
        body.put("deliveryBuilding", "北区12栋");

        var res = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(jsonBody(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return bodyOf(res).path("data").path("taskId").asLong();
    }

    private void acceptTaskOk(String token, long taskId, int version) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("version", version);
        mockMvc.perform(post("/api/tasks/" + taskId + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(jsonBody(body)))
                .andExpect(status().isOk());
    }

    // ───────── 异常流 1：自接禁止 ─────────

    @Test
    void accept_selfAccept_returns400() throws Exception {
        LoginResult user = setupUser("13900004001");
        long taskId = publishTask(user.accessToken());

        mockMvc.perform(post("/api/tasks/" + taskId + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(user.accessToken()))
                        .content(jsonBody(Map.of("version", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4003));
    }

    // ───────── 异常流 2：接单上限 ─────────

    @Test
    void accept_exceedLimit_returns409() throws Exception {
        LoginResult publisher = setupUser("13900004002");
        LoginResult accepter = setupUser("13900004003");

        long t1 = publishTask(publisher.accessToken());
        long t2 = publishTask(publisher.accessToken());
        long t3 = publishTask(publisher.accessToken());

        // 接前 2 个，达到默认上限 2
        acceptTaskOk(accepter.accessToken(), t1, 0);
        acceptTaskOk(accepter.accessToken(), t2, 0);

        // 第 3 个应被拒绝
        mockMvc.perform(post("/api/tasks/" + t3 + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(accepter.accessToken()))
                        .content(jsonBody(Map.of("version", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(4005));
    }

    // ───────── 异常流 3：状态机违法（已接单任务再次接单）───

    @Test
    void accept_alreadyAccepted_returns409() throws Exception {
        LoginResult publisher = setupUser("13900004004");
        LoginResult accepter1 = setupUser("13900004005");
        LoginResult accepter2 = setupUser("13900004006");

        long taskId = publishTask(publisher.accessToken());

        // accepter1 抢到
        acceptTaskOk(accepter1.accessToken(), taskId, 0);

        // accepter2 再抢 → 状态违法（已是 IN_PROGRESS）
        mockMvc.perform(post("/api/tasks/" + taskId + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(accepter2.accessToken()))
                        .content(jsonBody(Map.of("version", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(4006));
    }

    // ───────── 异常流 4：越权编辑 ─────────

    @Test
    void update_notPublisher_returns403() throws Exception {
        LoginResult publisher = setupUser("13900004007");
        LoginResult other = setupUser("13900004008");

        long taskId = publishTask(publisher.accessToken());

        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(other.accessToken()))
                        .content(jsonBody(Map.of("remark", "越权修改"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4007));
    }

    // ───────── 异常流 5：越权取消 ─────────

    @Test
    void cancel_notInvolved_returns403() throws Exception {
        LoginResult publisher = setupUser("13900004009");
        LoginResult other = setupUser("13900004010");

        long taskId = publishTask(publisher.accessToken());

        mockMvc.perform(post("/api/tasks/" + taskId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(other.accessToken()))
                        .content(jsonBody(Map.of("reason", "越权取消"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4008));
    }
}
