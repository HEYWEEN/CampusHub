-- Flyway V19 — 学号去重支持（auth_verification.student_no_hash）
--
-- 背景：学号以 AES-256-GCM 密文落库（随机 IV），同一学号每次加密结果不同，
--   无法直接按密文查重，导致两个账号用同一学号都能通过认证（bug 14）。
-- 方案：新增确定性索引列 student_no_hash = HMAC-SHA256(规范化学号)，
--   与 auth_user.phone_hmac 同套机制（AesUtil.hmacIndex，Base64-url 43 字符）。
--   提交 / 审核通过时校验：同 hash 不能被两个账号同时处于 APPROVED。
-- 不设 UNIQUE：REJECTED 历史记录、同一用户多次提交都可能重复，去重在 service 层按状态判定。

ALTER TABLE auth_verification
    ADD COLUMN student_no_hash VARCHAR(64) NULL COMMENT 'HMAC-SHA256(规范化学号)，跨账号学号查重索引';

CREATE INDEX idx_auth_verification_student_no_hash
    ON auth_verification (student_no_hash);
