-- Flyway V6 — 补齐两张缺失的表（schema drift 修复）
--
-- 历史背景：
--   代码中已存在 @Entity SmsCode（auth_sms_code）和 UserAuditLog（user_audit_log），
--   但 V1~V5 的 migration 文件均未建表。fresh 环境启动时 Hibernate ddl-auto=validate
--   会抛 SchemaManagementException: missing table，后端无法启动。
--
--   推测原因：USER-02 / AUTH-SMS 实现期，开发者本地用 ddl-auto=update 让 Hibernate
--   自动建了表（所以本地能跑），但没回写 migration。CI 跑 H2 / 没人在 fresh MySQL
--   环境验证，因此长期掩盖。
--
-- 关联 Bug 记录：docs/P4/bug/何翌闻_2026-05-28.md #3

-- ════════════════════════════════════════════════════════════
-- auth_sms_code — 短信验证码记录（短期、含 TTL）
-- 实体：com.campushub.auth.entity.SmsCode
-- ════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS auth_sms_code (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    phone_hmac   VARCHAR(64)  NOT NULL COMMENT '手机号 HMAC-SHA256 索引（不存明文）',
    code         VARCHAR(8)   NOT NULL COMMENT '6 位验证码',
    scene        VARCHAR(32)  NOT NULL COMMENT 'SmsScene 枚举：LOGIN / REGISTER / ...',
    expire_at    DATETIME(6)  NOT NULL COMMENT 'TTL 截止时间，过期记录后台清理',
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    consumed_at  DATETIME(6)  NULL COMMENT '被 LoginByCode 消费时间，非空代表已用过',
    PRIMARY KEY (id),
    KEY idx_sms_phone_hmac_scene (phone_hmac, scene),
    KEY idx_sms_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='短信验证码（USER-AUTH·短期·含 TTL）';

-- ════════════════════════════════════════════════════════════
-- user_audit_log — 用户敏感操作审计
-- 实体：com.campushub.user.entity.UserAuditLog
-- ════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS user_audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    action        VARCHAR(32)  NOT NULL COMMENT 'AuditAction 枚举：NICKNAME_CHANGE / AVATAR_CHANGE / PRIVACY_TOGGLE',
    before_value  VARCHAR(500) NULL,
    after_value   VARCHAR(500) NULL,
    ip            VARCHAR(64)  NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_user_audit_user_id_created (user_id, created_at),
    KEY idx_user_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户敏感操作审计（USER-02）';
