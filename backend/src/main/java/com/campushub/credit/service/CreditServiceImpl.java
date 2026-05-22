package com.campushub.credit.service;

import com.campushub.common.exception.BizException;
import com.campushub.credit.api.CreditApi;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.entity.CreditDirection;
import com.campushub.credit.entity.CreditRecord;
import com.campushub.credit.entity.CreditScoreLog;
import com.campushub.credit.exception.CreditErrorCode;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.credit.repository.CreditScoreLogRepository;
import com.campushub.credit.strategy.ScoreStrategy;
import com.campushub.credit.strategy.ScoreStrategyRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credit 模块核心服务 —— 同时实现 {@link CreditApi} 给跨模块调用（CRD-01）。
 *
 * <p><b>幂等模型</b>：每个写操作传 bizKey，先查 {@code credit_record.biz_id} 是否已存在，
 * 已存在则静默短路（return），保证事件重投/重试安全（P3 §5）。
 *
 * <p><b>不变量</b>：point_balance / point_frozen 不出负，credit_score 夹紧 [0,120]——
 * 边界校验落在 {@link CreditAccount} 领域方法里。并发更新靠 @Version 乐观锁。
 *
 * <p><b>账户懒创建</b>：getScoreOf / 冻结等遇账户不存在时按「认证即初始 100」语义创建，
 * 不依赖外部种子流程。
 *
 * <p>注：{@link #settle} 当前实现为「收款方入账」单边语义；付款方冻结的释放（task 完成时
 * 发布者 point_frozen 扣除）需与 B 的 task 完成事件 payload 一并敲定，见 CRD-04。
 */
@Service
public class CreditServiceImpl implements CreditApi {

    private final CreditAccountRepository accountRepo;
    private final CreditRecordRepository recordRepo;
    private final CreditScoreLogRepository scoreLogRepo;
    private final ScoreStrategyRegistry scoreRegistry;

    public CreditServiceImpl(CreditAccountRepository accountRepo,
                             CreditRecordRepository recordRepo,
                             CreditScoreLogRepository scoreLogRepo,
                             ScoreStrategyRegistry scoreRegistry) {
        this.accountRepo = accountRepo;
        this.recordRepo = recordRepo;
        this.scoreLogRepo = scoreLogRepo;
        this.scoreRegistry = scoreRegistry;
    }

    @Override
    @Transactional
    public int getScoreOf(long userId) {
        return getOrCreateAccount(userId).getCreditScore();
    }

    @Override
    @Transactional
    public void freeze(long userId, int points, String bizKey) {
        requirePositive(points);
        if (alreadyDone(bizKey)) return;

        CreditAccount account = getOrCreateAccount(userId);
        if (account.getPointBalance() < points) {
            throw new BizException(CreditErrorCode.CREDIT_NOT_ENOUGH, "可用积分不足，无法冻结", 422);
        }
        account.freeze(points);
        accountRepo.save(account);
        recordRepo.save(new CreditRecord(userId, CreditDirection.FREEZE, -points, "FREEZE", bizKey));
    }

    @Override
    @Transactional
    public void unfreeze(long userId, int points, String bizKey) {
        requirePositive(points);
        if (alreadyDone(bizKey)) return;

        CreditAccount account = getOrCreateAccount(userId);
        if (account.getPointFrozen() < points) {
            // 冻结额不足通常是上游 bizKey 配对错误，按业务规则拒绝而非静默
            throw new BizException(CreditErrorCode.CREDIT_NOT_ENOUGH, "冻结额不足，无法解冻", 422);
        }
        account.unfreeze(points);
        accountRepo.save(account);
        recordRepo.save(new CreditRecord(userId, CreditDirection.UNFREEZE, points, "UNFREEZE", bizKey));
    }

    @Override
    @Transactional
    public void settle(long userId, int points, String bizKey) {
        requirePositive(points);
        if (alreadyDone(bizKey)) return;

        CreditAccount payee = getOrCreateAccount(userId);
        payee.credit(points);
        accountRepo.save(payee);
        recordRepo.save(new CreditRecord(userId, CreditDirection.SETTLE, points, "SETTLE", bizKey));
    }

    @Override
    @Transactional
    public void deduct(long userId, int delta, String reasonCode, String bizKey) {
        if (scoreLogRepo.existsByBizId(bizKey)) return; // 幂等

        // 已登记的系统规则：以 Strategy 的 delta 为权威，忽略入参 delta（避免调用方传错）；
        // 未登记（如管理员手动调整 FR-ADM-01）：用入参 delta。
        int effectiveDelta;
        if (scoreRegistry.contains(reasonCode)) {
            ScoreStrategy rule = scoreRegistry.resolve(reasonCode);
            effectiveDelta = rule.delta();
        } else {
            effectiveDelta = delta;
        }

        CreditAccount account = getOrCreateAccount(userId);
        int before = account.getCreditScore();
        int after = account.adjustScore(effectiveDelta); // 内部夹紧 [0,120]
        accountRepo.save(account);
        scoreLogRepo.save(new CreditScoreLog(userId, after - before, reasonCode, before, after, bizKey));
    }

    /** 信用分计分快捷入口：reasonCode 决定 delta（CRD-04 listener / 评价触发用）。 */
    @Transactional
    public void applyScore(long userId, String reasonCode, String bizKey) {
        deduct(userId, 0, reasonCode, bizKey);
    }

    // ---- 内部 ----

    /** 幂等短路：bizKey 已记流水则视为已完成。 */
    private boolean alreadyDone(String bizKey) {
        return recordRepo.existsByBizId(bizKey);
    }

    private CreditAccount getOrCreateAccount(long userId) {
        return accountRepo.findByUserId(userId)
                .orElseGet(() -> accountRepo.save(new CreditAccount(userId)));
    }

    private static void requirePositive(int points) {
        if (points <= 0) {
            throw new BizException(CreditErrorCode.CREDIT_NOT_ENOUGH, "积分数量必须为正", 422);
        }
    }
}
