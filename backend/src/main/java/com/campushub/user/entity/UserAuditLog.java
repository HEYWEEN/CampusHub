package com.campushub.user.entity;

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
 * user_audit_log：用户敏感操作审计（P4 看板 USER-02 要求）。
 *
 * 记录维度：动作类型 + 改动前/后值 + IP + 时间。
 * 后期可被 admin 模块的"操作回溯"功能查询。
 *
 * **不存隐私字段明文**：before/after 仅记开关状态、昵称变化等非敏感内容；
 * 涉及手机号 / 真实姓名变更（如有）走 AesUtil.encrypt 后存。
 */
@Entity
@Table(
    name = "user_audit_log",
    indexes = {
        @Index(name = "idx_user_audit_user_id_created", columnList = "user_id,created_at"),
        @Index(name = "idx_user_audit_action", columnList = "action")
    }
)
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 32, nullable = false)
    private AuditAction action;

    @Column(name = "before_value", length = 500)
    private String beforeValue;

    @Column(name = "after_value", length = 500)
    private String afterValue;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UserAuditLog() {}

    public UserAuditLog(Long userId, AuditAction action, String beforeValue, String afterValue, String ip) {
        this.userId = userId;
        this.action = action;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public AuditAction getAction() { return action; }
    public String getBeforeValue() { return beforeValue; }
    public String getAfterValue() { return afterValue; }
    public String getIp() { return ip; }
    public Instant getCreatedAt() { return createdAt; }

    public enum AuditAction {
        NICKNAME_CHANGE,
        AVATAR_CHANGE,
        PRIVACY_TOGGLE
    }
}
