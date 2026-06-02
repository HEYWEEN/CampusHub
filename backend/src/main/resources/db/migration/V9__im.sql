-- Flyway V9 — im 模块（站内私信）F-IM-01/02/04
-- 1-1 会话 im_conversation + 消息 im_message
-- 枚举列用 INT（对齐本机库 Hibernate validate 期望，勿用 TINYINT）

CREATE TABLE IF NOT EXISTS im_conversation (
  id             BIGINT     NOT NULL AUTO_INCREMENT,
  user_a_id      BIGINT     NOT NULL COMMENT '参与者较小 id',
  user_b_id      BIGINT     NOT NULL COMMENT '参与者较大 id',
  biz_type       VARCHAR(16) NULL COMMENT '关联业务类型 TASK/TRADE，直聊为 NULL',
  biz_id         BIGINT     NULL,
  last_msg_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  a_last_read_msg_id BIGINT NULL COMMENT 'user_a 已读到的最大消息 id',
  b_last_read_msg_id BIGINT NULL COMMENT 'user_b 已读到的最大消息 id',
  created_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_im_conv_pair (user_a_id, user_b_id),
  KEY idx_im_conv_a (user_a_id, last_msg_at),
  KEY idx_im_conv_b (user_b_id, last_msg_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='站内私信会话（F-IM-01）';

CREATE TABLE IF NOT EXISTS im_message (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT       NOT NULL,
  sender_id       BIGINT       NOT NULL COMMENT '发送者 id，0=系统消息',
  content_type    INT          NOT NULL DEFAULT 0 COMMENT '0=文本 1=图片 2=系统',
  content         VARCHAR(2000) NOT NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_im_msg_conv_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='站内私信消息（F-IM-02）';
