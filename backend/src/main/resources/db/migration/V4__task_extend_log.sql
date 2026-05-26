-- Flyway V4: task_extend_log 表（延长截止时间记录）
-- 维护人：B | 2026-05-25

CREATE TABLE IF NOT EXISTS task_extend_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    task_id          BIGINT       NOT NULL COMMENT '逻辑外键 task_order',
    old_deadline_at  DATETIME     NOT NULL,
    new_deadline_at  DATETIME     NOT NULL,
    extend_count     INT          NOT NULL COMMENT '第几次延长',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_extend_log_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
