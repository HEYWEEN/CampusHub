-- Flyway V14 — schema drift 修复（auth_user.phone_hmac）
--
-- 历史背景：
--   V1 把 phone_hmac 建成 CHAR(64)，但 entity AuthUser.phoneHmac 是
--   `@Column(length = 64) String`，Hibernate 映射为 VARCHAR(64)。
--   V7 已经按同样套路对齐过 phone_cipher / attachment_sha256，
--   只是漏了 phone_hmac 这一列。MySQL 9 + ddl-auto=validate 下，
--   CHAR vs VARCHAR 不匹配会 SchemaManagementException 启动失败。
--
-- 同步：V6 在 sms_code 上建的就是 VARCHAR(64)，本次只补 auth_user。

ALTER TABLE auth_user
    MODIFY COLUMN phone_hmac VARCHAR(64) NOT NULL COMMENT 'HMAC-SHA256(规范化手机号)，用于登录唯一匹配与索引查找';
