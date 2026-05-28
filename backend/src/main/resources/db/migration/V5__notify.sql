-- Flyway V5: notify_message 表（站内信）
-- 维护人：A | 2026-05-28
-- 参考：P3/04_核心类图.md §3.4 NotifyMessage、P3/03_包结构骨架.md §5

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
