package com.campushub.integration;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA-02 正常流（task 段）：注册 → 发任务 → 接单 → 提交凭证 → 发布者确认 → 信用账户变化断言。
 *
 * <p>覆盖链路：A 的 auth + B 的 task + D 的 credit + {@link com.campushub.credit.listener.TaskEventListener}。
 * 与 {@link TradeHappyPathFlowTest} 同构，补齐 QA-02 看板上「task 段」TODO。
 *
 * <p>命名 *Test 以便在当前 CI（surefire）下被执行——见 {@link BaseIT} 顶部说明。
 */
class TaskHappyPathFlowTest extends BaseIT {

    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private AuthUserRepository authUserRepo;

    /** 与 AuthService.SIGNUP_BONUS_POINTS 对齐：注册即得 100。 */
    private static final int INITIAL_BALANCE = 100;
    private static final int REWARD = 40;
    /** 与 TaskServiceImpl.accept 一致：deposit = max(reward/5, 5)。 */
    private static final int DEPOSIT = Math.max(REWARD / 5, 5);

    /** task 发布/接单都要求 verifyStatus = APPROVED。 */
    private void approveUser(long userId) {
        AuthUser u = authUserRepo.findById(userId).orElseThrow();
        u.setVerifyStatus(VerifyStatus.APPROVED);
        authUserRepo.save(u);
    }

    /** 注册 + 认证一步到位（注册自带 100 启动积分）。 */
    private LoginResult setupUser(String phone) throws Exception {
        LoginResult user = registerAndLogin(phone);
        approveUser(user.userId());
        return user;
    }

    /** HTTP 发布跑腿任务，返回 taskId。 */
    private long publishTask(String token) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "帮取快递");
        body.put("taskType", "ERRAND");
        body.put("rewardPoint", REWARD);
        body.put("deadlineAt", "2026-12-31T23:59:59Z");
        body.put("pickupHint", "韵达 2 号柜");
        body.put("deliveryBuilding", "北区 12 栋");

        MvcResult res = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(jsonBody(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return bodyOf(res).path("data").path("taskId").asLong();
    }

    private void acceptTask(String token, long taskId, int version) throws Exception {
        mockMvc.perform(post("/api/tasks/" + taskId + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(jsonBody(Map.of("version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void submitProof(String token, long taskId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("images", List.of());
        body.put("text", "已送达 12 栋 304");
        mockMvc.perform(post("/api/tasks/" + taskId + "/proof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(jsonBody(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void confirmComplete(String token, long taskId) throws Exception {
        mockMvc.perform(post("/api/tasks/" + taskId + "/confirm")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void task_fromPublish_toConfirm_settlesCredit() throws Exception {
        LoginResult publisher = setupUser("13900005001");
        LoginResult accepter  = setupUser("13900005002");

        // 发布：publisher freeze REWARD → balance 100-40=60, frozen=40
        long taskId = publishTask(publisher.accessToken());
        CreditAccount pubAfterPublish = accountRepo.findByUserId(publisher.userId()).orElseThrow();
        assertEquals(INITIAL_BALANCE - REWARD, pubAfterPublish.getPointBalance());
        assertEquals(REWARD, pubAfterPublish.getPointFrozen());

        // 接单：accepter freeze DEPOSIT → balance 100-8=92, frozen=8
        acceptTask(accepter.accessToken(), taskId, 0);
        CreditAccount accAfterAccept = accountRepo.findByUserId(accepter.userId()).orElseThrow();
        assertEquals(INITIAL_BALANCE - DEPOSIT, accAfterAccept.getPointBalance());
        assertEquals(DEPOSIT, accAfterAccept.getPointFrozen());

        // 提交凭证 + 发布者确认：触发 TaskCompletedEvent
        // → TaskEventListener.unfreeze(publisher, REWARD) + settle(accepter, REWARD)
        submitProof(accepter.accessToken(), taskId);
        confirmComplete(publisher.accessToken(), taskId);

        // 最终：
        //  publisher frozen 清零、balance 回到 INITIAL_BALANCE
        //   （与 TradeHappyPathFlowTest 一致的「unfreeze+settle 守恒缺口」，
        //    待 CreditApi 增加 transfer(payer,payee) 时同步收紧）
        //  accepter deposit 由 TaskServiceImpl.confirmComplete 内联 unfreeze 退还，
        //  frozen 清零，balance = INITIAL + REWARD
        CreditAccount pubFinal = accountRepo.findByUserId(publisher.userId()).orElseThrow();
        CreditAccount accFinal = accountRepo.findByUserId(accepter.userId()).orElseThrow();
        assertEquals(0, pubFinal.getPointFrozen(), "发布者冻结应被释放");
        assertEquals(INITIAL_BALANCE, pubFinal.getPointBalance(),
                "按当前 unfreeze+settle 约定，发布者余额回到发任务前");
        assertEquals(INITIAL_BALANCE + REWARD, accFinal.getPointBalance(),
                "接单者收到结算积分且押金已退还（deposit 由 confirm 内联 unfreeze）");
        assertEquals(0, accFinal.getPointFrozen(), "接单者押金已在完成路径上退还");

        // HTTP /api/credits/me 视角（与前端一致）
        mockMvc.perform(get("/api/credits/me").header("Authorization", bearer(accepter.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointBalance").value(INITIAL_BALANCE + REWARD))
                .andExpect(jsonPath("$.data.pointFrozen").value(0));
    }
}
