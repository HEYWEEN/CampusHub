-- CampusHub 全量 DDL（MySQL 8）— 运行时副本
-- 与 docs/P3/数据库/schema.sql 保持同步（C 维护）；变更时两处同时提交。
-- 手动建库: mysql -u root -p < backend/src/main/resources/db/schema.sql
-- 应用启动时由 Flyway 执行 db/migration/V*.sql，本文件供人工重放与评审。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS campushub
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE campushub;

-- ---------------------------------------------------------------------------
-- auth 模块（表级 FK 仅模块内）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_user (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  phone_hmac        CHAR(64)     NOT NULL COMMENT 'HMAC-SHA256(规范化手机号)，用于登录唯一匹配与索引查找',
  phone_cipher      VARBINARY(512) NOT NULL COMMENT 'AES-256-GCM 密文；严禁日志与 API 输出',
  password_hash     VARCHAR(255) NULL COMMENT 'BCrypt cost=10；验证码-only 用户可为 NULL',
  verify_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=guest 1=pending 2=approved 3=rejected（与 Java Converter 对齐）',
  banned            TINYINT      NOT NULL DEFAULT 0 COMMENT '0=正常 1=封禁',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_user_phone_hmac (phone_hmac),
  KEY idx_auth_user_verify_status (verify_status),
  KEY idx_auth_user_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='鉴权用户；手机号仅存密文+HMAC，禁止明文列';

CREATE TABLE IF NOT EXISTS auth_verification (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL,
  status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0=pending 1=approved 2=rejected',
  real_name_cipher  VARBINARY(512) NULL COMMENT 'AES-256-GCM；业务表禁冗余真实姓名',
  student_no_cipher VARBINARY(512) NULL,
  id_card_cipher    VARBINARY(512) NULL,
  reject_reason     VARCHAR(500) NULL,
  attachment_sha256 JSON        NULL COMMENT '证件图 SHA-256 列表 JSON；原图在对象存储',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  KEY idx_auth_verification_user_id (user_id),
  KEY idx_auth_verification_status_created (status, created_at),
  CONSTRAINT fk_auth_verification_user FOREIGN KEY (user_id) REFERENCES auth_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- user 模块（与 auth 逻辑关联，禁止 DB 级跨前缀外键）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_profile (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL COMMENT '逻辑外键 → auth_user.id，不加 FK',
  nickname          VARCHAR(64)  NOT NULL,
  avatar_url        VARCHAR(512) NULL,
  hide_publish_hist TINYINT      NOT NULL DEFAULT 1 COMMENT '1=隐藏发布历史（默认开）',
  hide_accept_hist  TINYINT      NOT NULL DEFAULT 1,
  hide_course_reviews TINYINT    NOT NULL DEFAULT 1,
  daily_accept_limit TINYINT     NOT NULL DEFAULT 2 COMMENT '个人接单上限 1~3，默认 2',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_profile_user_id (user_id),
  KEY idx_user_profile_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- task 模块
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_order (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  publisher_id      BIGINT       NOT NULL COMMENT '逻辑外键 user',
  assignee_id       BIGINT       NULL COMMENT '逻辑外键 user',
  title             VARCHAR(120) NOT NULL,
  task_type         TINYINT      NOT NULL COMMENT '枚举：跑腿/互助/辅导',
  status            TINYINT      NOT NULL COMMENT 'State 机状态，TINYINT+Converter',
  reward_point      INT          NOT NULL,
  deadline_at       DATETIME     NOT NULL,
  pickup_hint       VARCHAR(200) NOT NULL,
  delivery_building VARCHAR(120) NOT NULL,
  remark            VARCHAR(500) NULL,
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  KEY idx_task_order_publisher_status (publisher_id, status),
  KEY idx_task_order_assignee_status (assignee_id, status),
  KEY idx_task_order_status_deadline (status, deadline_at),
  KEY idx_task_order_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS task_attachment (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  task_id           BIGINT       NOT NULL,
  url               VARCHAR(512) NOT NULL,
  kind              TINYINT      NOT NULL DEFAULT 0 COMMENT '0=image 1=other',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  KEY idx_task_attachment_task_id (task_id),
  CONSTRAINT fk_task_attachment_task FOREIGN KEY (task_id) REFERENCES task_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS task_review (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  task_id           BIGINT       NOT NULL,
  reviewer_id       BIGINT       NOT NULL COMMENT '通常为发布者',
  reviewee_id       BIGINT       NOT NULL COMMENT '通常为接单者',
  rating            TINYINT      NOT NULL COMMENT '1~5',
  comment           VARCHAR(500) NULL,
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_review_task_reviewer (task_id, reviewer_id),
  KEY idx_task_review_reviewee (reviewee_id, created_at),
  CONSTRAINT fk_task_review_task FOREIGN KEY (task_id) REFERENCES task_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- credit 模块
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS credit_account (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL COMMENT '逻辑外键 user',
  point_balance     INT          NOT NULL DEFAULT 0,
  point_frozen      INT          NOT NULL DEFAULT 0,
  credit_score      INT          NOT NULL DEFAULT 100 COMMENT '0~120',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_account_user_id (user_id),
  KEY idx_credit_account_credit_score (credit_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS credit_record (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL,
  direction         TINYINT      NOT NULL COMMENT '冻结/解冻/结算等，枚举由 Converter 映射',
  delta             INT          NOT NULL,
  reason_code       VARCHAR(64)  NOT NULL,
  biz_id            VARCHAR(128) NOT NULL COMMENT '幂等键，如 task:123:freeze',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_record_biz (biz_id),
  KEY idx_credit_record_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS credit_score_log (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL,
  delta             INT          NOT NULL COMMENT '信用分有符号变化',
  reason_code       VARCHAR(64)  NOT NULL COMMENT '计分规则，见 ScoreRule 枚举',
  before_score      INT          NOT NULL,
  after_score       INT          NOT NULL,
  biz_id            VARCHAR(128) NOT NULL COMMENT '幂等键，如 review:999:malicious',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_score_log_biz (biz_id),
  KEY idx_credit_score_log_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- trade 模块
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trade_item (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  seller_id         BIGINT       NOT NULL,
  title             VARCHAR(200) NOT NULL,
  description       VARCHAR(2000) NULL,
  price_point       INT          NOT NULL DEFAULT 0,
  pickup_location_type TINYINT   NOT NULL DEFAULT 1 COMMENT '0=精确宿舍 1=楼栋范围 2=面交',
  pickup_location_detail VARCHAR(200) NULL,
  status            TINYINT      NOT NULL DEFAULT 0 COMMENT '上架/下架/交易中',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  KEY idx_trade_item_seller_status (seller_id, status),
  KEY idx_trade_item_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE IF NOT EXISTS trade_order (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  item_id           BIGINT       NOT NULL,
  buyer_id          BIGINT       NOT NULL,
  seller_id         BIGINT       NOT NULL,
  status            TINYINT      NOT NULL COMMENT '议价中/交易中/完成等',
  negotiated_price_point INT     NOT NULL DEFAULT 0,
  freeze_point      INT          NOT NULL DEFAULT 0,
  buyer_confirmed   TINYINT      NOT NULL DEFAULT 0,
  seller_confirmed  TINYINT      NOT NULL DEFAULT 0,
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  KEY idx_trade_order_buyer_status (buyer_id, status),
  KEY idx_trade_order_seller_status (seller_id, status),
  KEY idx_trade_order_item_id (item_id),
  CONSTRAINT fk_trade_order_item FOREIGN KEY (item_id) REFERENCES trade_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- edu 模块
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edu_tutor_task (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  publisher_id    BIGINT       NOT NULL,
  subject         VARCHAR(120) NOT NULL,
  description     VARCHAR(2000) NOT NULL,
  reward_point    INT          NOT NULL,
  status          TINYINT      NOT NULL DEFAULT 0,
  version         INT          NOT NULL DEFAULT 0,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at      DATETIME     NULL,
  creator_id      BIGINT       NULL,
  updater_id      BIGINT       NULL,
  PRIMARY KEY (id),
  KEY idx_edu_tutor_task_publisher (publisher_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS edu_forbidden_word_hit (
  user_id         BIGINT       NOT NULL,
  hit_count       INT          NOT NULL DEFAULT 0,
  cooldown_until  DATETIME     NULL COMMENT '冷静期结束时间',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS admin_forbidden_word (
  id        BIGINT       NOT NULL AUTO_INCREMENT,
  word      VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_forbidden_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
