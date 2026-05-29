-- Flyway V7 — schema drift 修复（schema_audit C-1 / C-2）
--
-- 历史背景：
--   1. auth_user.phone_cipher 在 V1 定义为 VARBINARY(512)，但 entity 字段是
--      `String phoneCipher`（AES-GCM 密文 Base64 → ASCII 文本），语义其实是 VARCHAR。
--      Hibernate validate 模式下，类型 VARBINARY vs VARCHAR 不匹配会 SchemaManagementException。
--      之前临时用 ddl-auto=update 让 Hibernate 自动 ALTER，但未回写 migration。
--
--   2. auth_verification.attachment_sha256 在 V1 定义为 JSON 列，但 entity
--      `@Column(columnDefinition = "TEXT") String attachmentSha256Json` 期望 TEXT。
--      validate 模式同样会失败。
--
-- 本脚本统一对齐：把两列改成 entity 期望的类型，然后才能把 ddl-auto 切回 validate。
--
-- 关联：docs/P4/bug/schema_audit_2026-05-28.md C-1 / C-2

-- ════════════════════════════════════════════════════════════
-- auth_user.phone_cipher: VARBINARY(512) → VARCHAR(512)
-- ════════════════════════════════════════════════════════════
-- 注意：MySQL 9 的 MODIFY COLUMN 会按位将 VARBINARY 字节流读成 VARCHAR
-- （charset = utf8mb4），AES-GCM 密文是 Base64 ASCII 文本本来就 utf8mb4 安全。
ALTER TABLE auth_user
    MODIFY COLUMN phone_cipher VARCHAR(512) NOT NULL COMMENT 'AES-256-GCM 密文 Base64';

-- ════════════════════════════════════════════════════════════
-- auth_verification.attachment_sha256: JSON → TEXT
-- ════════════════════════════════════════════════════════════
-- 内容当前是 JSON 数组字符串（schema_audit A-12 修复后改为 URL 列表 JSON），
-- 用 TEXT 存储足够；不再依赖 MySQL JSON 函数（实际上 service 也是 ObjectMapper 解析）。
ALTER TABLE auth_verification
    MODIFY COLUMN attachment_sha256 TEXT NULL COMMENT 'URL 列表 JSON（原 sha256 JSON）';
