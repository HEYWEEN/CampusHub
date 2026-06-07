-- Flyway V20 — 用户可隐藏「已撤销的差评」「申诉记录」
--
-- 背景：差评申诉通过后评价被撤销（task_review.voided=1），但仍占着「我收到的评价」
--   列表；申诉处理完后记录也一直留在「我的申诉」。用户希望能主动删掉这些。
-- 方案：软隐藏（仅从本人视图移除，不物理删除——评价行还关联评价人/申诉，保留审计）。
--   boolean 列按项目约定用 TINYINT NOT NULL DEFAULT 0（preferred_boolean_jdbc_type=TINYINT）。

ALTER TABLE task_review
    ADD COLUMN hidden_by_reviewee TINYINT NOT NULL DEFAULT 0
        COMMENT '被评价人是否已从「我收到的评价」隐藏（仅已撤销 voided 的差评可隐藏）';

ALTER TABLE credit_appeal
    ADD COLUMN hidden TINYINT NOT NULL DEFAULT 0
        COMMENT '申诉人是否已从「我的申诉」隐藏该记录（仅已处理的申诉可隐藏）';
