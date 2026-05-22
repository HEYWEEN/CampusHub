package com.campushub.credit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * credit_score_log：信用分变更日志（CRD-03）。每次信用分调整落一行，记 before/after，
 * 用于 180 天信用曲线展示（F-CREDIT-02）与审计举证。
 *
 * <p>幂等：{@code biz_id} 唯一键，同一计分事件重投不重复加减。
 * <p>注：本表 schema.sql 由 D 在 CRD-03 补充，已 @C（schema owner）同步。
 */
@Entity
@Table(
    name = "credit_score_log",
    indexes = {
        @Index(name = "uk_credit_score_log_biz", columnList = "biz_id", unique = true),
        @Index(name = "idx_credit_score_log_user_created", columnList = "user_id, created_at")
    }
)
public class CreditScoreLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Column(name = "reason_code", length = 64, nullable = false)
    private String reasonCode;

    @Column(name = "before_score", nullable = false)
    private int beforeScore;

    @Column(name = "after_score", nullable = false)
    private int afterScore;

    /** 幂等键，如 review:999:malicious */
    @Column(name = "biz_id", length = 128, nullable = false, unique = true)
    private String bizId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public CreditScoreLog() {}

    public CreditScoreLog(Long userId, int delta, String reasonCode,
                          int beforeScore, int afterScore, String bizId) {
        this.userId = userId;
        this.delta = delta;
        this.reasonCode = reasonCode;
        this.beforeScore = beforeScore;
        this.afterScore = afterScore;
        this.bizId = bizId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public int getDelta() { return delta; }
    public String getReasonCode() { return reasonCode; }
    public int getBeforeScore() { return beforeScore; }
    public int getAfterScore() { return afterScore; }
    public String getBizId() { return bizId; }
    public Instant getCreatedAt() { return createdAt; }
}
