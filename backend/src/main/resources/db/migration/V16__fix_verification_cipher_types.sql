-- Flyway V16 — auth_verification 加密列从 VARBINARY 改为 VARCHAR
--
-- 历史背景：
--   V7 已经把 auth_user.phone_cipher 从 VARBINARY(512) 改成 VARCHAR(512)
--   （AES-GCM 密文是 Base64 ASCII 文本，按文本存反而更合适），
--   但 auth_verification 表里三列同类型字段被漏改：
--     real_name_cipher / student_no_cipher / id_card_cipher
--   entity 字段都是 `String`（Base64），Hibernate validate 期望 VARCHAR。

ALTER TABLE auth_verification
    MODIFY COLUMN real_name_cipher VARCHAR(512) NULL COMMENT 'AES-256-GCM Base64';

ALTER TABLE auth_verification
    MODIFY COLUMN student_no_cipher VARCHAR(512) NULL COMMENT 'AES-256-GCM Base64';

ALTER TABLE auth_verification
    MODIFY COLUMN id_card_cipher VARCHAR(512) NULL COMMENT 'AES-256-GCM Base64';
