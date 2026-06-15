-- CampusHub 数据库 DDL（MariaDB / MySQL 8）
-- 依据：docs/P3/01_统一规约.md §3（审计字段、主键、跨模块无 FK、命名、utf8mb4）
--
-- ⚠️ 权威来源声明（2026-06-14 对齐）：
--   本文件为 P3 设计快照，**最终物理结构以 Flyway migration 为权威**：
--   backend/src/main/resources/db/migration/V1__init_schema.sql ~ V20__hide_review_appeal.sql。
--   本文件已按 migration 累积效果回写对齐，主要差异点如下（供答辩对照）：
--   1. 枚举/状态整数列统一 **INT**（非 TINYINT）——V15__enum_columns_to_int 修正；
--      Hibernate ddl-auto=validate 期望 INTEGER，TINYINT 会启动报错。仅 boolean 列保留 TINYINT
--      （preferred_boolean_jdbc_type=TINYINT，见 application.properties）。
--   2. 加密/HMAC 列为 VARCHAR（非 VARBINARY/CHAR/JSON）——V7/V14/V16 修正（密文 Base64 文本落库）。
--   3. P3 之后迭代新增表（V4~V20）见文件末「§B 迭代新增表」：
--      auth_sms_code / user_audit_log / task_extend_log / team_recruit / team_application /
--      im_conversation / im_message / credit_appeal / report_case / report_decision /
--      agent_conversation / agent_message / trade_offer。
-- 覆盖：鉴权 / 用户资料 / 任务与附件 / 信用账户与流水 / 二手订单 / 任务评价（§A 初版核心表）
--      + 组队 / 私信 / 申诉 / 举报 / AI 助手 / 砍价（§B 迭代新增表）

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
  phone_hmac        VARCHAR(64)  NOT NULL COMMENT 'HMAC-SHA256(规范化手机号)，登录唯一匹配（V14 由 CHAR→VARCHAR）',
  phone_cipher      VARCHAR(512) NOT NULL COMMENT 'AES-256-GCM 密文(Base64)；严禁日志与 API 输出（V7 由 VARBINARY→VARCHAR）',
  password_hash     VARCHAR(255) NULL COMMENT 'BCrypt cost=10；验证码-only 用户可为 NULL',
  verify_status     INT          NOT NULL DEFAULT 0 COMMENT '0=guest 1=pending 2=approved 3=rejected（V15 由 TINYINT→INT）',
  banned            TINYINT      NOT NULL DEFAULT 0 COMMENT '0=正常 1=封禁（boolean 保留 TINYINT）',
  role              INT          NOT NULL DEFAULT 0 COMMENT '0=USER 1=ADMIN 2=SUPER_ADMIN（V11 新增）',
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
  status            INT          NOT NULL DEFAULT 0 COMMENT '0=pending 1=approved 2=rejected（V15 由 TINYINT→INT）',
  real_name_cipher  VARCHAR(512) NULL COMMENT 'AES-256-GCM(Base64)；业务表禁冗余真实姓名（V16 由 VARBINARY→VARCHAR）',
  student_no_cipher VARCHAR(512) NULL COMMENT 'V16 由 VARBINARY→VARCHAR',
  student_no_hash   VARCHAR(64)  NULL COMMENT 'HMAC-SHA256(规范化学号)，跨账号学号查重索引（V19 新增）',
  id_card_cipher    VARCHAR(512) NULL COMMENT 'V16 由 VARBINARY→VARCHAR',
  reject_reason     VARCHAR(500) NULL,
  attachment_sha256 TEXT         NULL COMMENT '证件图 SHA-256 列表 JSON 文本；原图在对象存储（V7 由 JSON→TEXT）',
  version           INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at        DATETIME     NULL,
  creator_id        BIGINT       NULL,
  updater_id        BIGINT       NULL,
  PRIMARY KEY (id),
  KEY idx_auth_verification_user_id (user_id),
  KEY idx_auth_verification_status_created (status, created_at),
  KEY idx_auth_verification_student_no_hash (student_no_hash),
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
  daily_accept_limit INT         NOT NULL DEFAULT 2 COMMENT '个人接单上限 1~3，默认 2（V15 由 TINYINT→INT）',
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
-- task 模块（表名遵循 §3.4 `<module>_<entity>` 约定：task_order）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_order (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  publisher_id      BIGINT       NOT NULL COMMENT '逻辑外键 user',
  assignee_id       BIGINT       NULL COMMENT '逻辑外键 user',
  title             VARCHAR(120) NOT NULL,
  task_type         INT          NOT NULL COMMENT '0=ERRAND 跑腿 1=MUTUAL_HELP 互助 2=TUTOR 辅导（V15 由 TINYINT→INT）',
  status            INT          NOT NULL COMMENT 'State 机状态 0=PENDING_ACCEPT 1=IN_PROGRESS 2=WAIT_CONFIRM 3=COMPLETED 4=CANCELED 5=EXPIRED（V15 由 TINYINT→INT）',
  reward_point      INT          NOT NULL COMMENT '悬赏积分 1~500',
  deadline_at       DATETIME     NOT NULL,
  pickup_hint       VARCHAR(200) NOT NULL,
  delivery_building VARCHAR(120) NOT NULL,
  remark            VARCHAR(500) NULL,
  proof_text        VARCHAR(500) NULL COMMENT '接单者提交凭证时的完成说明（V18 新增）',
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
  kind              INT          NOT NULL DEFAULT 0 COMMENT '0=image 1=other（V15 由 TINYINT→INT）',
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
  rating            INT          NOT NULL COMMENT '1~5（V15 由 TINYINT→INT）',
  comment           VARCHAR(500) NULL,
  voided            TINYINT      NOT NULL DEFAULT 0 COMMENT '0=正常 1=申诉通过已撤销（V10 新增）',
  hidden_by_reviewee TINYINT     NOT NULL DEFAULT 0 COMMENT '被评价人是否已从「我收到的评价」隐藏（V20 新增）',
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
  direction         INT          NOT NULL COMMENT '0=FREEZE 1=UNFREEZE 2=SETTLE 3=DEDUCT（V15 由 TINYINT→INT）',
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

-- CRD-03：信用分变更日志（D 补充，已与 schema owner 同步）。用于 180 天信用曲线 + 审计举证。
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
-- trade 模块（订单详情 API 对应）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trade_item (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  seller_id         BIGINT       NOT NULL,
  title             VARCHAR(200) NOT NULL,
  description       VARCHAR(2000) NULL,
  price_point       INT          NOT NULL DEFAULT 0,
  pickup_location_type INT       NOT NULL DEFAULT 1 COMMENT '0=精确宿舍 1=楼栋范围 2=面交（V15 由 TINYINT→INT）',
  pickup_location_detail VARCHAR(200) NULL,
  status            INT          NOT NULL DEFAULT 0 COMMENT '0=ON_SALE 上架 1=OFF_SALE 下架 2=IN_TRADE 交易中（V15 由 TINYINT→INT）',
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
  sort_order   INT          NOT NULL DEFAULT 0 COMMENT 'V15 由 TINYINT→INT',
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
  status            INT          NOT NULL COMMENT '0=IN_TRADE 1=BUYER_CONFIRMED 2=SELLER_CONFIRMED 3=COMPLETED 4=CANCELED（议价走 trade_offer；V15 由 TINYINT→INT）',
  negotiated_price_point INT     NOT NULL DEFAULT 0 COMMENT '议价成交价',
  freeze_point      INT          NOT NULL DEFAULT 0,
  buyer_confirmed   TINYINT      NOT NULL DEFAULT 0 COMMENT 'boolean 保留 TINYINT',
  seller_confirmed  TINYINT      NOT NULL DEFAULT 0 COMMENT 'boolean 保留 TINYINT',
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
-- edu 模块（辅导发布 + 违禁词）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edu_tutor_task (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  publisher_id    BIGINT       NOT NULL,
  subject         VARCHAR(120) NOT NULL,
  description     VARCHAR(2000) NOT NULL,
  reward_point    INT          NOT NULL,
  status          INT          NOT NULL DEFAULT 0 COMMENT '辅导任务状态（V15 由 TINYINT→INT）',
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

-- notify 模块（A 维护；与 V5__notify.sql 同步）
CREATE TABLE IF NOT EXISTS notify_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL COMMENT '接收人 user_id',
  type        VARCHAR(32)  NOT NULL COMMENT 'TASK_ACCEPTED / TASK_COMPLETED / TASK_CANCELED / TASK_EXPIRED ...',
  title       VARCHAR(128) NOT NULL,
  body        VARCHAR(512) NOT NULL,
  biz_key     VARCHAR(96)  NOT NULL COMMENT '幂等键，建议 type:taskId:userId；同一事件同一接收人 24h 内自然去重',
  read_at     DATETIME     NULL     COMMENT 'NULL=未读',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notify_biz_key (biz_key),
  KEY idx_notify_user_unread (user_id, read_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===========================================================================
-- §B 迭代新增表（P3 之后 V4~V20 补充；枚举列统一 INT，boolean 列 TINYINT）
-- 权威定义见对应 migration 文件，本节为对齐快照。
-- ===========================================================================

-- auth_sms_code — 短信验证码（含 TTL）。V6。注意 scene 用 VARCHAR 字符串存储（@Enumerated STRING）。
CREATE TABLE IF NOT EXISTS auth_sms_code (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  phone_hmac   VARCHAR(64)  NOT NULL COMMENT '手机号 HMAC-SHA256',
  code         VARCHAR(8)   NOT NULL,
  scene        VARCHAR(32)  NOT NULL COMMENT 'SmsScene 枚举（字符串存储）',
  expire_at    DATETIME(6)  NOT NULL,
  created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  consumed_at  DATETIME(6)  NULL,
  PRIMARY KEY (id),
  KEY idx_sms_phone_hmac_scene (phone_hmac, scene),
  KEY idx_sms_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- user_audit_log — 用户敏感操作审计。V6。action 用 VARCHAR 字符串存储（@Enumerated STRING）。
CREATE TABLE IF NOT EXISTS user_audit_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  action        VARCHAR(32)  NOT NULL COMMENT 'AuditAction 枚举（字符串存储）',
  before_value  VARCHAR(500) NULL,
  after_value   VARCHAR(500) NULL,
  ip            VARCHAR(64)  NULL,
  created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_user_audit_user_id_created (user_id, created_at),
  KEY idx_user_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- task_extend_log — 任务延长截止时间记录。V4。
CREATE TABLE IF NOT EXISTS task_extend_log (
  id               BIGINT   NOT NULL AUTO_INCREMENT,
  task_id          BIGINT   NOT NULL COMMENT '逻辑外键 task_order',
  old_deadline_at  DATETIME NOT NULL,
  new_deadline_at  DATETIME NOT NULL,
  extend_count     INT      NOT NULL COMMENT '第几次延长',
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_extend_log_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- team_recruit / team_application — 组队招募与申请。V8。
CREATE TABLE IF NOT EXISTS team_recruit (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  creator_id    BIGINT        NOT NULL COMMENT '队长（发帖人）',
  title         VARCHAR(120)  NOT NULL,
  description   VARCHAR(1000) NULL,
  skill_tags    VARCHAR(255)  NULL COMMENT '技能标签，逗号分隔，1~5 个',
  total_size    INT           NOT NULL COMMENT '队伍总人数（含队长）',
  current_size  INT           NOT NULL DEFAULT 1,
  status        INT           NOT NULL DEFAULT 0 COMMENT '0=招募中 1=已满员 2=已关闭',
  version       INT           NOT NULL DEFAULT 0,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at    DATETIME      NULL,
  PRIMARY KEY (id),
  KEY idx_team_recruit_status_created (status, created_at),
  KEY idx_team_recruit_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_application (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  recruit_id    BIGINT        NOT NULL,
  applicant_id  BIGINT        NOT NULL,
  message       VARCHAR(500)  NULL,
  status        INT           NOT NULL DEFAULT 0 COMMENT '0=待审 1=通过 2=拒绝',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_app_recruit_status (recruit_id, status),
  KEY idx_team_app_applicant (applicant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- im_conversation / im_message — 站内私信。V9。未读数由 a/b_last_read_msg_id 计算，无独立计数表。
CREATE TABLE IF NOT EXISTS im_conversation (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  user_a_id      BIGINT      NOT NULL COMMENT '参与者较小 id',
  user_b_id      BIGINT      NOT NULL COMMENT '参与者较大 id',
  biz_type       VARCHAR(16) NULL COMMENT 'TASK/TRADE，直聊为 NULL',
  biz_id         BIGINT      NULL,
  last_msg_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  a_last_read_msg_id BIGINT  NULL,
  b_last_read_msg_id BIGINT  NULL,
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_im_conv_pair (user_a_id, user_b_id),
  KEY idx_im_conv_a (user_a_id, last_msg_at),
  KEY idx_im_conv_b (user_b_id, last_msg_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS im_message (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT        NOT NULL,
  sender_id       BIGINT        NOT NULL COMMENT '0=系统消息',
  content_type    INT           NOT NULL DEFAULT 0 COMMENT '0=文本 1=图片 2=系统',
  content         VARCHAR(2000) NOT NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_im_msg_conv_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- credit_appeal — 信用差评申诉。V10（+V20 hidden）。
CREATE TABLE IF NOT EXISTS credit_appeal (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  review_id     BIGINT        NOT NULL COMMENT '被申诉的 task_review.id',
  appellant_id  BIGINT        NOT NULL COMMENT '申诉人（被差评一方）',
  reason        VARCHAR(500)  NOT NULL,
  evidence_urls VARCHAR(1000) NULL COMMENT '证据图 URL，逗号分隔，≤5',
  status        INT           NOT NULL DEFAULT 0 COMMENT '0=待审 1=通过 2=驳回',
  resolver_id   BIGINT        NULL,
  resolve_note  VARCHAR(500)  NULL,
  hidden        TINYINT       NOT NULL DEFAULT 0 COMMENT '申诉人是否已隐藏该记录（V20 新增）',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_credit_appeal_appellant (appellant_id, created_at),
  KEY idx_credit_appeal_review_status (review_id, status),
  KEY idx_credit_appeal_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- report_case / report_decision — 举报与仲裁存档。V12。decision_type 仅追加。
CREATE TABLE IF NOT EXISTS report_case (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  reporter_id      BIGINT        NOT NULL,
  target_type      INT           NOT NULL COMMENT '0=TASK 1=TRADE 2=USER',
  target_id        BIGINT        NOT NULL,
  reported_user_id BIGINT        NULL,
  reason_category  VARCHAR(64)   NOT NULL,
  description      VARCHAR(1000) NULL,
  evidence_urls    VARCHAR(1000) NULL,
  status           INT           NOT NULL DEFAULT 0 COMMENT '0=待处理 1=已处理 2=已驳回',
  handler_id       BIGINT        NULL,
  decision_type    INT           NULL COMMENT '0=DISMISS 1=WARN 2=PENALIZE',
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_report_case_status (status, created_at),
  KEY idx_report_case_reporter (reporter_id, created_at),
  KEY idx_report_case_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS report_decision (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  case_id        BIGINT        NOT NULL,
  admin_id       BIGINT        NOT NULL,
  decision_type  INT           NOT NULL COMMENT '0=DISMISS 1=WARN 2=PENALIZE',
  penalty_points INT           NOT NULL DEFAULT 0,
  reason         VARCHAR(1000) NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_report_decision_case (case_id),
  KEY idx_report_decision_admin (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- agent_conversation / agent_message — AI 助手对话持久化。V13。role 用 INT(ORDINAL)。
CREATE TABLE IF NOT EXISTS agent_conversation (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  title       VARCHAR(100) NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_agent_conv_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS agent_message (
  id              BIGINT   NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT   NOT NULL,
  role            INT      NOT NULL COMMENT '0=user 1=assistant',
  content         TEXT     NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_agent_msg_conv (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- trade_offer — 二手砍价（offer/counter-offer）。V17。accept 后生成 trade_order。
CREATE TABLE IF NOT EXISTS trade_offer (
  id              BIGINT   NOT NULL AUTO_INCREMENT,
  item_id         BIGINT   NOT NULL,
  buyer_id        BIGINT   NOT NULL,
  seller_id       BIGINT   NOT NULL,
  price_point     INT      NOT NULL,
  status          INT      NOT NULL DEFAULT 0 COMMENT '0=PENDING 1=ACCEPTED 2=REJECTED 3=CANCELED',
  awaiting_party  INT      NOT NULL DEFAULT 1 COMMENT '0=BUYER 1=SELLER 的回合',
  order_id        BIGINT   NULL COMMENT 'accept 后回填成单的 trade_order.id',
  version         INT      NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at      DATETIME NULL,
  creator_id      BIGINT   NULL,
  updater_id      BIGINT   NULL,
  PRIMARY KEY (id),
  KEY idx_trade_offer_buyer_status  (buyer_id, status),
  KEY idx_trade_offer_seller_status (seller_id, status),
  KEY idx_trade_offer_item_id       (item_id),
  CONSTRAINT fk_trade_offer_item FOREIGN KEY (item_id) REFERENCES trade_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
