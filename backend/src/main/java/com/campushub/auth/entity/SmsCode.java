package com.campushub.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * auth_sms_code：短信验证码记录（短期、含 TTL）。
 *
 * 隐私约束（P3 §9.1）：手机号**永不**以明文落库，本表只存 HMAC 索引。
 * 短信场景 + HMAC 联合查询 → 拿到未过期验证码做校验。
 *
 * 表设计要点：
 *   - phone_hmac 上加索引，验证流程走 hmac 等值匹配
 *   - expire_at 用作 TTL；过期记录由后台定时清理（后续 task，本期手动）
 *   - consumed_at 标记是否已被消费，避免一码多用
 */
@Entity
@Table(
    name = "auth_sms_code",
    indexes = {
        @Index(name = "idx_sms_phone_hmac_scene", columnList = "phone_hmac,scene"),
        @Index(name = "idx_sms_expire_at", columnList = "expire_at")
    }
)
public class SmsCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 手机号 HMAC-SHA256 索引（不含明文） */
    @Column(name = "phone_hmac", length = 64, nullable = false)
    private String phoneHmac;

    @Column(name = "code", length = 8, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene", length = 32, nullable = false)
    private SmsScene scene;

    @Column(name = "expire_at", nullable = false)
    private Instant expireAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** 被 LoginByCode 消费后写入；非空代表已用过 */
    @Column(name = "consumed_at")
    private Instant consumedAt;

    public SmsCode() {}

    public SmsCode(String phoneHmac, String code, SmsScene scene, Instant expireAt) {
        this.phoneHmac = phoneHmac;
        this.code = code;
        this.scene = scene;
        this.expireAt = expireAt;
        this.createdAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expireAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void markConsumed(Instant when) {
        this.consumedAt = when;
    }

    public Long getId() { return id; }
    public String getPhoneHmac() { return phoneHmac; }
    public String getCode() { return code; }
    public SmsScene getScene() { return scene; }
    public Instant getExpireAt() { return expireAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConsumedAt() { return consumedAt; }
}
