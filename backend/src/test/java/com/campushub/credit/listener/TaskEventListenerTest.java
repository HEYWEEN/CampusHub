package com.campushub.credit.listener;

import com.campushub.credit.api.CreditApi;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.task.event.TaskCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * CRD-04 task 段单测：直接调用 listener 验证 unfreeze + settle 两段语义 + bizKey 幂等。
 *
 * <p>与 {@link TradeEventListenerTest} 同构；只测 listener 自身行为，不拉 task service。
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskEventListenerTest {

    @Autowired private TaskEventListener listener;
    @Autowired private CreditApi credit;
    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private CreditRecordRepository recordRepo;

    private static final long PUBLISHER = 8001L;
    private static final long ACCEPTER = 8002L;
    private static final long TASK = 9101L;
    private static final int REWARD = 40;

    @BeforeEach
    void cleanup() {
        recordRepo.deleteAll();
        accountRepo.deleteAll();
    }

    /** seed 用户余额（懒创建账户 + deposit）。 */
    private void seedBalance(long userId, int points) {
        CreditAccount acc = accountRepo.findByUserId(userId).orElseGet(() -> {
            CreditAccount a = new CreditAccount(userId);
            return accountRepo.save(a);
        });
        acc.deposit(points);
        accountRepo.save(acc);
    }

    @Test
    void onCompleted_unfreezesPublisher_andSettlesAccepter() {
        // 准备：发布者有 100 余额并先 freeze 40 悬赏（模拟 TaskServiceImpl.publish）
        seedBalance(PUBLISHER, 100);
        credit.freeze(PUBLISHER, REWARD, "task:" + TASK + ":freeze");
        assertEquals(60, accountRepo.findByUserId(PUBLISHER).orElseThrow().getPointBalance());
        assertEquals(REWARD, accountRepo.findByUserId(PUBLISHER).orElseThrow().getPointFrozen());

        // act：触发完成事件
        listener.onTaskCompleted(
                new TaskCompletedEvent(TASK, PUBLISHER, ACCEPTER, REWARD, 1L));

        // assert：发布者 frozen 归零并退回 balance，接单者入账
        CreditAccount publisher = accountRepo.findByUserId(PUBLISHER).orElseThrow();
        CreditAccount accepter = accountRepo.findByUserId(ACCEPTER).orElseThrow();
        assertEquals(0, publisher.getPointFrozen(), "发布者冻结应被释放");
        assertEquals(100, publisher.getPointBalance(),
                "按当前 unfreeze+settle 约定，发布者余额回到发任务前");
        assertEquals(REWARD, accepter.getPointBalance(), "接单者应收到结算积分");
    }

    @Test
    void onCompleted_replayedEvent_isIdempotent() {
        seedBalance(PUBLISHER, 100);
        credit.freeze(PUBLISHER, REWARD, "task:" + TASK + ":freeze");

        TaskCompletedEvent evt = new TaskCompletedEvent(TASK, PUBLISHER, ACCEPTER, REWARD, 1L);
        listener.onTaskCompleted(evt);
        listener.onTaskCompleted(evt); // 重投

        // 余额/流水不应被加第二次
        assertEquals(REWARD, accountRepo.findByUserId(ACCEPTER).orElseThrow().getPointBalance());
        assertEquals(100, accountRepo.findByUserId(PUBLISHER).orElseThrow().getPointBalance());
        // freeze + settle_unfreeze + settle 各一条
        assertEquals(3, recordRepo.findAll().size(), "重投不应写第二份 credit_record");
    }

    @Test
    void listener_isWiredAsSpringBean() {
        // 间接验证 bean 已注册（由 @SpringBootTest 装配）
        assertNotNull(listener);
    }
}
