-- Flyway V8 — team 模块（比赛 / 课设组队）F-TEAM-01~04
-- 招募帖 team_recruit + 申请 team_application
-- 列类型严格对齐 entity（ddl-auto=validate）

CREATE TABLE IF NOT EXISTS team_recruit (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  creator_id    BIGINT        NOT NULL COMMENT '队长（发帖人）',
  title         VARCHAR(120)  NOT NULL,
  description   VARCHAR(1000) NULL,
  skill_tags    VARCHAR(255)  NULL COMMENT '技能标签，逗号分隔，1~5 个',
  total_size    INT           NOT NULL COMMENT '队伍总人数（含队长）',
  current_size  INT           NOT NULL DEFAULT 1 COMMENT '当前人数（队长占 1）',
  status        INT           NOT NULL DEFAULT 0 COMMENT '0=招募中 1=已满员 2=已关闭',
  version       INT           NOT NULL DEFAULT 0,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at    DATETIME      NULL,
  PRIMARY KEY (id),
  KEY idx_team_recruit_status_created (status, created_at),
  KEY idx_team_recruit_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='组队招募帖（F-TEAM-01）';

CREATE TABLE IF NOT EXISTS team_application (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  recruit_id    BIGINT        NOT NULL,
  applicant_id  BIGINT        NOT NULL,
  message       VARCHAR(500)  NULL COMMENT '申请留言',
  status        INT           NOT NULL DEFAULT 0 COMMENT '0=待审 1=通过 2=拒绝',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_app_recruit_status (recruit_id, status),
  KEY idx_team_app_applicant (applicant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='组队申请（F-TEAM-02/03）';
