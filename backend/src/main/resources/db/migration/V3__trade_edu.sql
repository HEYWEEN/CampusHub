-- Flyway V3: trade + edu 增量 DDL（兼容 Hibernate 早期建表库）
-- 新环境若已执行 V1 全量脚本，本迁移因 IF NOT EXISTS 幂等跳过。
-- 维护人：C | 2026-05-23

-- trade_item 补列
ALTER TABLE trade_item
    ADD COLUMN IF NOT EXISTS description VARCHAR(2000) NULL COMMENT '商品描述' AFTER title,
    ADD COLUMN IF NOT EXISTS price_point INT NOT NULL DEFAULT 0 COMMENT '标价（积分）' AFTER description,
    ADD COLUMN IF NOT EXISTS pickup_location_type TINYINT NOT NULL DEFAULT 1 COMMENT '0=精确宿舍 1=楼栋范围 2=面交' AFTER price_point,
    ADD COLUMN IF NOT EXISTS pickup_location_detail VARCHAR(200) NULL COMMENT '取货地点描述' AFTER pickup_location_type;

-- trade_item_image
CREATE TABLE IF NOT EXISTS trade_item_image (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    item_id      BIGINT       NOT NULL,
    url          VARCHAR(512) NOT NULL,
    sort_order   TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_trade_item_image_item_id (item_id),
    CONSTRAINT fk_trade_item_image_item FOREIGN KEY (item_id) REFERENCES trade_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- trade_order 补列
ALTER TABLE trade_order
    ADD COLUMN IF NOT EXISTS negotiated_price_point INT NOT NULL DEFAULT 0 COMMENT '议价后成交价' AFTER status,
    ADD COLUMN IF NOT EXISTS buyer_confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '买家已确认' AFTER freeze_point,
    ADD COLUMN IF NOT EXISTS seller_confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '卖家已确认' AFTER buyer_confirmed;

-- edu_tutor_task
CREATE TABLE IF NOT EXISTS edu_tutor_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    publisher_id    BIGINT       NOT NULL,
    subject         VARCHAR(120) NOT NULL,
    description     VARCHAR(2000) NOT NULL,
    reward_point    INT          NOT NULL,
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待接单',
    version         INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL,
    creator_id      BIGINT       NULL,
    updater_id      BIGINT       NULL,
    PRIMARY KEY (id),
    KEY idx_edu_tutor_task_publisher (publisher_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- edu_forbidden_word_hit
CREATE TABLE IF NOT EXISTS edu_forbidden_word_hit (
    user_id         BIGINT       NOT NULL,
    hit_count       INT          NOT NULL DEFAULT 0,
    cooldown_until  DATETIME     NULL COMMENT '冷静期结束时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- admin_forbidden_word
CREATE TABLE IF NOT EXISTS admin_forbidden_word (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    word      VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_forbidden_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
