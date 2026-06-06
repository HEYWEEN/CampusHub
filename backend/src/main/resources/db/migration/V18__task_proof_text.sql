-- Flyway V18 — task_order 增加「完成说明」列 proof_text
--
-- 背景：
--   接单者提交凭证（submitProof）时会填一段「完成说明」文字，
--   但此前后端只在响应里回传、未持久化，发布者确认时看不到。
--   现把它落到 task_order.proof_text，详情接口随凭证图一并返回。
--   entity Task.proofText 为 String，Hibernate validate 期望 VARCHAR。

ALTER TABLE task_order
    ADD COLUMN proof_text VARCHAR(500) NULL COMMENT '接单者提交凭证时的完成说明';
