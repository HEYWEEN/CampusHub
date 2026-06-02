-- Flyway V10 — 信用申诉 F-CREDIT-05~07
-- credit_appeal 表 + task_review 加 voided 标记（申诉通过后撤销该评价）
-- 枚举 status 用 INT；boolean voided 用 TINYINT（对齐项目 boolean→TINYINT 约定）

CREATE TABLE IF NOT EXISTS credit_appeal (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  review_id     BIGINT        NOT NULL COMMENT '被申诉的 task_review.id',
  appellant_id  BIGINT        NOT NULL COMMENT '申诉人（被差评的一方）',
  reason        VARCHAR(500)  NOT NULL,
  evidence_urls VARCHAR(1000) NULL COMMENT '证据图 URL，逗号分隔，≤5',
  status        INT           NOT NULL DEFAULT 0 COMMENT '0=待审 1=通过 2=驳回',
  resolver_id   BIGINT        NULL COMMENT '处理的管理员',
  resolve_note  VARCHAR(500)  NULL,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_credit_appeal_appellant (appellant_id, created_at),
  KEY idx_credit_appeal_review_status (review_id, status),
  KEY idx_credit_appeal_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='信用差评申诉（F-CREDIT-05~07）';

ALTER TABLE task_review
  ADD COLUMN voided TINYINT NOT NULL DEFAULT 0 COMMENT '0=正常 1=申诉通过已撤销';
