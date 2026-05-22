package com.campushub.credit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * credit_record：积分/信用流水（P3 §1 schema.sql）。每次资金/信用变更落一行，作审计 + 幂等。
 *
 * <p><b>幂等核心</b>：{@code biz_id} 唯一键 {@code uk_credit_record_biz}。同一 bizKey 第二次写入会撞唯一约束，
 * service 层据此短路（{@code existsByBizId}），保证事件重投/重试不会重复扣加。
 *
 * <p>{@code delta} 约定：记 point_balance 的有符号变化（FREEZE 为负、UNFREEZE/SETTLE 为正）；
 * DEDUCT 行记 credit_score 的有符号变化。
 */
@Entity
@Table(
    name = "credit_record",
    indexes = {
        @Index(name = "uk_credit_record_biz", columnList = "biz_id", unique = true),
        @Index(name = "idx_credit_record_user_created", columnList = "user_id, created_at")
    }
)
public class CreditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Convert(converter = CreditDirectionConverter.class)
    @Column(name = "direction", nullable = false)
    private CreditDirection direction;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Column(name = "reason_code", length = 64, nullable = false)
    private String reasonCode;

    /** 幂等键，如 task:123:freeze */
    @Column(name = "biz_id", length = 128, nullable = false, unique = true)
    private String bizId;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CreditRecord() {}

    public CreditRecord(Long userId, CreditDirection direction, int delta, String reasonCode, String bizId) {
        this.userId = userId;
        this.direction = direction;
        this.delta = delta;
        this.reasonCode = reasonCode;
        this.bizId = bizId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public CreditDirection getDirection() { return direction; }
    public int getDelta() { return delta; }
    public String getReasonCode() { return reasonCode; }
    public String getBizId() { return bizId; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
