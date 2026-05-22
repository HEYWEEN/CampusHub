package com.campushub.credit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * credit_account：用户积分 + 信用分账户（P3 §1 schema.sql，每用户一行）。
 *
 * <p>不变量（CRD-01 完成标准「账户余额不会出负」）：
 * <ul>
 *   <li>point_balance ≥ 0、point_frozen ≥ 0 恒成立</li>
 *   <li>credit_score 夹紧 [0, 120]</li>
 * </ul>
 *
 * <p>跨模块只用逻辑外键 {@code user_id}（无 JPA @ManyToOne，遵 P3 §1）。
 * 并发更新（同一账户同时 freeze/settle）靠 @Version 乐观锁保护。
 */
@Entity
@Table(
    name = "credit_account",
    indexes = {
        @Index(name = "uk_credit_account_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_credit_account_credit_score", columnList = "credit_score")
    }
)
public class CreditAccount {

    /** 信用分上下界（P3 §3.7） */
    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 120;
    /** 认证通过后初始信用分 */
    public static final int INITIAL_SCORE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "point_balance", nullable = false)
    private int pointBalance = 0;

    @Column(name = "point_frozen", nullable = false)
    private int pointFrozen = 0;

    @Column(name = "credit_score", nullable = false)
    private int creditScore = INITIAL_SCORE;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CreditAccount() {}

    public CreditAccount(Long userId) {
        this.userId = userId;
    }

    // ---- 领域行为（所有金额变更都在这里做边界校验，杜绝出负） ----

    /** 充值/初始化可用积分（管理端发放 / 测试种子）。 */
    public void deposit(int points) {
        requirePositive(points);
        this.pointBalance += points;
        touch();
    }

    /** 冻结：可用 → 冻结。余额不足由调用方先校验（这里再兜底）。 */
    public void freeze(int points) {
        requirePositive(points);
        if (pointBalance < points) {
            throw new IllegalStateException("冻结超出可用余额");
        }
        this.pointBalance -= points;
        this.pointFrozen += points;
        touch();
    }

    /** 解冻：冻结 → 可用（取消退回）。 */
    public void unfreeze(int points) {
        requirePositive(points);
        if (pointFrozen < points) {
            throw new IllegalStateException("解冻超出冻结额");
        }
        this.pointFrozen -= points;
        this.pointBalance += points;
        touch();
    }

    /** 收款：结算时积分计入可用余额（收款方）。 */
    public void credit(int points) {
        requirePositive(points);
        this.pointBalance += points;
        touch();
    }

    /**
     * 信用分调整并夹紧 [0,120]。
     * @return 实际调整后的分数
     */
    public int adjustScore(int delta) {
        int next = this.creditScore + delta;
        if (next < MIN_SCORE) next = MIN_SCORE;
        if (next > MAX_SCORE) next = MAX_SCORE;
        this.creditScore = next;
        touch();
        return next;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static void requirePositive(int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("积分数量必须为正：" + points);
        }
    }

    // ---- getters ----
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public int getPointBalance() { return pointBalance; }
    public int getPointFrozen() { return pointFrozen; }
    public int getCreditScore() { return creditScore; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
