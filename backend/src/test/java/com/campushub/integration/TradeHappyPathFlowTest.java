package com.campushub.integration;

import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA-02 正常流（trade 段）：注册 → 发商品 → 议价下单 → 双方确认 → 信用账户变化断言。
 *
 * <p>覆盖链路：A 的 auth + C 的 trade + D 的 credit + {@link com.campushub.credit.listener.TradeEventListener}。
 * task 段全链路 HappyPath 待 B 的 task 接口落地后再补（同文件追加用例即可）。
 *
 * <p>命名 *Test 是为了在当前 CI（surefire）下被执行——见 {@link BaseIT} 顶部说明。
 */
class TradeHappyPathFlowTest extends BaseIT {

    @Autowired private CreditAccountRepository accountRepo;

    private static final int INITIAL_BALANCE = 100;
    private static final int PRICE = 30;

    /** seed 用户初始余额（绕过尚未实现的"积分发放"路径）。 */
    private void seedBalance(long userId, int points) {
        CreditAccount acc = accountRepo.findByUserId(userId).orElseGet(() -> {
            CreditAccount a = new CreditAccount(userId);
            return accountRepo.save(a);
        });
        acc.deposit(points);
        accountRepo.save(acc);
    }

    /** 通过 HTTP 发布一件二手商品，返回 itemId。 */
    private long publishItem(String sellerToken) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/api/trade/items")
                        .param("title", "二手 Python 教材")
                        .param("description", "九成新")
                        .param("pricePoint", String.valueOf(PRICE))
                        .param("pickupLocationType", "BUILDING_RANGE")
                        .param("pickupLocationDetail", "三号楼大厅")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return bodyOf(res).path("data").path("id").asLong();
    }

    /** 通过 HTTP 下单，返回 orderId。 */
    private long placeOrder(String buyerToken, long itemId, int price) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("itemId", itemId);
        body.put("negotiatedPricePoint", price);
        MvcResult res = mockMvc.perform(post("/api/trade/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(buyerToken))
                        .content(jsonBody(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return bodyOf(res).path("data").path("id").asLong();
    }

    private void confirmOrder(String token, long orderId) throws Exception {
        mockMvc.perform(post("/api/trade/orders/" + orderId + "/confirm")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void tradeOrder_fromPublish_toBothConfirm_settlesCredit() throws Exception {
        LoginResult buyer = registerAndLogin("13900003001");
        LoginResult seller = registerAndLogin("13900003002");
        seedBalance(buyer.userId(), INITIAL_BALANCE);

        long itemId = publishItem(seller.accessToken());
        long orderId = placeOrder(buyer.accessToken(), itemId, PRICE);

        // 下单后：买家余额扣 30、冻结 30；卖家不动
        CreditAccount buyerAccAfterFreeze = accountRepo.findByUserId(buyer.userId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(INITIAL_BALANCE - PRICE, buyerAccAfterFreeze.getPointBalance());
        org.junit.jupiter.api.Assertions.assertEquals(PRICE, buyerAccAfterFreeze.getPointFrozen());

        // 双方确认：触发 TradeOrderCompletedEvent → TradeEventListener.unfreeze + settle
        confirmOrder(buyer.accessToken(), orderId);
        confirmOrder(seller.accessToken(), orderId);

        // 最终：买家冻结清零；卖家入账（按当前 unfreeze+settle 约定，买家余额回到初值——
        // 此条断言与 TradeEventListener javadoc 中描述的「已知守恒缺口」一致；
        // 等 CreditApi 增加 transfer(payer,payee) 时同步收紧）。
        CreditAccount buyerFinal = accountRepo.findByUserId(buyer.userId()).orElseThrow();
        CreditAccount sellerFinal = accountRepo.findByUserId(seller.userId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, buyerFinal.getPointFrozen(), "买家冻结应释放");
        org.junit.jupiter.api.Assertions.assertEquals(INITIAL_BALANCE, buyerFinal.getPointBalance());
        org.junit.jupiter.api.Assertions.assertEquals(PRICE, sellerFinal.getPointBalance(), "卖家应收到结算积分");

        // 同步通过 HTTP /api/credits/me 验证（前端真实视角）
        mockMvc.perform(get("/api/credits/me").header("Authorization", bearer(seller.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointBalance").value(PRICE));
    }
}
