-- Flyway V15 — V1 中的「枚举/整数」列从 TINYINT 改为 INT
--
-- 历史背景：
--   V1 时期把所有枚举/小整数列都写成 TINYINT；后来 V9 注释明确了
--   「枚举列用 INT（对齐本机库 Hibernate validate 期望，勿用 TINYINT）」，
--   但 V1 留下的 TINYINT 列没有回写。MySQL 9 + ddl-auto=validate 下，
--   entity 的 enum / int 字段 Hibernate 默认期望 JDBC INTEGER，
--   找到 TINYINT 就 SchemaManagementException 启动失败。
--
-- 范围：只改 entity 字段是 enum 或 int 的列；boolean 列继续用 TINYINT
--   （application.properties 已设 preferred_boolean_jdbc_type=TINYINT 兼容）。
--
-- 保留 boolean 不动：
--   auth_user.banned, user_profile.hide_publish_hist / hide_accept_hist /
--   hide_course_reviews, trade_order.buyer_confirmed / seller_confirmed

-- auth 模块
ALTER TABLE auth_user
    MODIFY COLUMN verify_status INT NOT NULL DEFAULT 0 COMMENT '0=guest 1=pending 2=approved 3=rejected';

ALTER TABLE auth_verification
    MODIFY COLUMN status INT NOT NULL DEFAULT 0 COMMENT '0=pending 1=approved 2=rejected';

-- user 模块
ALTER TABLE user_profile
    MODIFY COLUMN daily_accept_limit INT NOT NULL DEFAULT 2 COMMENT '个人接单上限 1~3，默认 2';

-- task 模块
ALTER TABLE task_order
    MODIFY COLUMN task_type INT NOT NULL COMMENT '枚举：跑腿/互助/辅导';

ALTER TABLE task_order
    MODIFY COLUMN status INT NOT NULL COMMENT 'State 机状态';

ALTER TABLE task_attachment
    MODIFY COLUMN kind INT NOT NULL DEFAULT 0 COMMENT '0=image 1=other';

ALTER TABLE task_review
    MODIFY COLUMN rating INT NOT NULL COMMENT '1~5';

-- credit 模块
ALTER TABLE credit_record
    MODIFY COLUMN direction INT NOT NULL COMMENT '冻结/解冻/结算等';

-- trade 模块
ALTER TABLE trade_item
    MODIFY COLUMN pickup_location_type INT NOT NULL DEFAULT 1 COMMENT '0=精确宿舍 1=楼栋范围 2=面交';

ALTER TABLE trade_item
    MODIFY COLUMN status INT NOT NULL DEFAULT 0 COMMENT '上架/下架/交易中';

ALTER TABLE trade_item_image
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0;

ALTER TABLE trade_order
    MODIFY COLUMN status INT NOT NULL COMMENT '议价中/交易中/完成等';

-- edu 模块
ALTER TABLE edu_tutor_task
    MODIFY COLUMN status INT NOT NULL DEFAULT 0;
