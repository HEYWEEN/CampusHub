package com.campushub.credit.controller;

import com.campushub.credit.api.CreditApi;
import com.campushub.integration.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-CREDIT-01 / 08：GET /api/credits/me 与 /me/records。
 * 复用 {@link BaseIT}（清表 + mintToken）。命名 *Test 以在 CI 执行。
 */
class CreditControllerTest extends BaseIT {

    @Autowired private CreditApi creditApi;

    @Test
    void getMyCredit_newUser_returnsScore100_andCanPublish() throws Exception {
        long uid = 7771L;
        mockMvc.perform(get("/api/credits/me").header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(uid))
                .andExpect(jsonPath("$.data.creditScore").value(100))
                .andExpect(jsonPath("$.data.pointBalance").value(0))
                .andExpect(jsonPath("$.data.pointFrozen").value(0))
                .andExpect(jsonPath("$.data.canPublish").value(true))
                .andExpect(jsonPath("$.data.canAccept").value(true))
                .andExpect(jsonPath("$.data.dailyAcceptLimit").value(3));
    }

    @Test
    void getMyCredit_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/credits/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyRecords_returnsPagedFlow() throws Exception {
        long uid = 7772L;
        creditApi.settle(uid, 50, "task:100:settle");
        creditApi.settle(uid, 30, "task:101:settle");

        mockMvc.perform(get("/api/credits/me/records?page=1&size=10")
                        .header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].direction").value("SETTLE"));
    }

    @Test
    void getMyRecords_emptyWhenNoFlow() throws Exception {
        mockMvc.perform(get("/api/credits/me/records")
                        .header("Authorization", bearer(mintToken(7773L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }
}
