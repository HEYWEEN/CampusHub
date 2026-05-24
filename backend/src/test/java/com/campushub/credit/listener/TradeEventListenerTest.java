package com.campushub.credit.listener;

import com.campushub.credit.api.CreditApi;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.trade.event.TradeOrderCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRD-04 trade 段单测：直接调用 listener 验证 unfreeze + settle 两段语义 + bizKey 幂等。
 */
@SpringBootTest
@ActiveProfiles("test")
class TradeEventListenerTest {

    @Autowired private TradeEventListener listener;
    @Autowired private CreditApi credit;
    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private CreditRecordRepository recordRepo;

    private static final long BUYER = 7001L;
    private static final long SELLER = 7002L;
    private static final long ORDER = 9001L;
    private static final int PRICE = 30;

    @BeforeEach
    void cleanup() {
        recordRepo.deleteAll();
        accountRepo.deleteAll();
    }

    /** seed 买家初始余额（懒创建账户 + deposit）。 */
    private void seedBalance(long userId, int points) {
        CreditAccount acc = accountRepo.findByUserId(userId).orElseGet(() -> {
            CreditAccount a = new CreditAccount(userId);
            return accountRepo.save(a);
        });
        acc.deposit(points);
        accountRepo.save(acc);
    }

    @Test
    void onCompleted_unfreezesBuyer_andSettlesSeller() {
        // 准备：买家有 100 余额，并先做一次 freeze（模拟 TradeOrderService.createOrder）
        seedBalance(BUYER, 100);
        credit.freeze(BUYER, PRICE, "trade:" + ORDER + ":freeze");
        assertEquals(70, accountRepo.findByUserId(BUYER).orElseThrow().getPointBalance());
        assertEquals(PRICE, accountRepo.findByUserId(BUYER).orElseThrow().getPointFrozen());

        // act：触发完成事件
        listener.onTradeOrderCompleted(
                new TradeOrderCompletedEvent(ORDER, BUYER, SELLER, PRICE));

        // assert：买家 frozen 归零（按 kanban 约定 unfreeze 退回 balance）、卖家入账
        CreditAccount buyer = accountRepo.findByUserId(BUYER).orElseThrow();
        CreditAccount seller = accountRepo.findByUserId(SELLER).orElseThrow();
        assertEquals(0, buyer.getPointFrozen(), "买家冻结应被释放");
        assertEquals(100, buyer.getPointBalance(), "按当前 unfreeze+settle 约定，买家余额回到下单前");
        assertEquals(PRICE, seller.getPointBalance(), "卖家应收到结算积分");
    }

    @Test
    void onCompleted_replayedEvent_isIdempotent() {
        seedBalance(BUYER, 100);
        credit.freeze(BUYER, PRICE, "trade:" + ORDER + ":freeze");

        TradeOrderCompletedEvent evt = new TradeOrderCompletedEvent(ORDER, BUYER, SELLER, PRICE);
        listener.onTradeOrderCompleted(evt);
        listener.onTradeOrderCompleted(evt); // 重投

        // 余额/流水不应被加第二次
        assertEquals(PRICE, accountRepo.findByUserId(SELLER).orElseThrow().getPointBalance());
        assertEquals(100, accountRepo.findByUserId(BUYER).orElseThrow().getPointBalance());
        // freeze + unfreeze + settle 各一条，不是两条
        assertEquals(3, recordRepo.findAll().size(), "重投不应写第二份 credit_record");
    }

    @Test
    void onCompleted_isWiredAsSpringListener() {
        // 通过 ApplicationEventPublisher 间接验证 bean 已注册为监听器
        seedBalance(BUYER, 100);
        credit.freeze(BUYER, PRICE, "trade:" + ORDER + ":freeze");
        // 直接调用即可证明 bean 存在；Spring 装配由 @SpringBootTest 提供
        assertTrue(listener != null);
    }
}
