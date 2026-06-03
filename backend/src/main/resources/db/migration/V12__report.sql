-- Flyway V12 — 举报 / 仲裁（F-REPORT-01/03/04，简化版）
-- 枚举列（target_type / status / decision_type）走 INT（ORDINAL），对齐项目约定；
-- 跨模块仅逻辑外键，不加 FK 约束。

CREATE TABLE IF NOT EXISTS report_case (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  reporter_id      BIGINT        NOT NULL,
  target_type      INT           NOT NULL COMMENT '0=TASK 1=TRADE 2=USER',
  target_id        BIGINT        NOT NULL,
  reported_user_id BIGINT        NULL COMMENT '被举报方用户(USER=本人/TASK=发布者/TRADE=NULL)',
  reason_category  VARCHAR(64)   NOT NULL,
  description      VARCHAR(1000) NULL,
  evidence_urls    VARCHAR(1000) NULL COMMENT '证据图 URL，逗号分隔，≤5',
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

-- 仲裁存档：仅追加、不更新不删除（F-REPORT-04）
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
