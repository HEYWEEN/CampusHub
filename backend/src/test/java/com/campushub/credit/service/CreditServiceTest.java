package com.campushub.credit.service;

import com.campushub.credit.api.CreditApi;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRD-01 单测：CreditApi 四方法 + bizKey 幂等 + 余额不出负。
 * 用 @SpringBootTest + H2（test profile），与 auth 模块测试风格一致。
 */
@SpringBootTest
@ActiveProfiles("test")
class CreditServiceTest {

    @Autowired private CreditApi credit;
    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private CreditRecordRepository recordRepo;

    private static final long USER = 1001L;

    @BeforeEach
    void cleanup() {
        recordRepo.deleteAll();
        accountRepo.deleteAll();
    }

    /** 给用户预置一个有余额的账户。 */
    private void seedBalance(long userId, int balance) {
        CreditAccount acct = new CreditAccount(userId);
        acct.deposit(balance);
        accountRepo.save(acct);
    }

    private CreditAccount reload(long userId) {
        return accountRepo.findByUserId(userId).orElseThrow();
    }

    @Test
    void getScoreOf_newUser_returns100_andCreatesAccount() {
        int score = credit.getScoreOf(USER);
        assertEquals(100, score);
        assertTrue(accountRepo.existsByUserId(USER), "首次查询应懒创建账户");
    }

    @Test
    void freeze_deductsBalance_andLocksFrozen() {
        seedBalance(USER, 100);
        credit.freeze(USER, 30, "task:1:freeze");

        CreditAccount acct = reload(USER);
        assertEquals(70, acct.getPointBalance());
        assertEquals(30, acct.getPointFrozen());
    }

    @Test
    void freeze_insufficientBalance_throws422() {
        seedBalance(USER, 20);
        BizException ex = assertThrows(BizException.class,
                () -> credit.freeze(USER, 50, "task:2:freeze"));
        assertEquals(422, ex.getHttpStatus());
        // 余额不动
        assertEquals(20, reload(USER).getPointBalance());
    }

    @Test
    void freeze_sameBizKeyTwice_isIdempotent() {
        seedBalance(USER, 100);
        credit.freeze(USER, 30, "task:3:freeze");
        credit.freeze(USER, 30, "task:3:freeze"); // 重投

        CreditAccount acct = reload(USER);
        assertEquals(70, acct.getPointBalance(), "重复 bizKey 不应二次扣减");
        assertEquals(30, acct.getPointFrozen());
        assertEquals(1, recordRepo.count(), "幂等下只应有 1 条流水");
    }

    @Test
    void unfreeze_returnsFrozenToBalance() {
        seedBalance(USER, 100);
        credit.freeze(USER, 40, "task:4:freeze");
        credit.unfreeze(USER, 40, "task:4:unfreeze");

        CreditAccount acct = reload(USER);
        assertEquals(100, acct.getPointBalance());
        assertEquals(0, acct.getPointFrozen());
    }

    @Test
    void settle_creditsPayeeBalance() {
        // 收款方初始无账户，结算应懒创建并入账
        credit.settle(USER, 50, "task:5:settle");
        assertEquals(50, reload(USER).getPointBalance());
    }

    @Test
    void settle_sameBizKeyTwice_isIdempotent() {
        credit.settle(USER, 50, "task:6:settle");
        credit.settle(USER, 50, "task:6:settle");
        assertEquals(50, reload(USER).getPointBalance(), "重复结算不应二次入账");
    }

    @Test
    void balance_neverGoesNegative_acrossFreezeUnfreeze() {
        seedBalance(USER, 10);
        credit.freeze(USER, 10, "task:7:freeze");
        assertEquals(0, reload(USER).getPointBalance());
        // 再冻结应被余额不足拦截
        assertThrows(BizException.class, () -> credit.freeze(USER, 1, "task:7b:freeze"));
        assertTrue(reload(USER).getPointBalance() >= 0);
    }
}
