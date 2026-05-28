package com.campushub.notify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * notify_message：站内信（P3 §3.4 类图 NotifyMessage）。
 *
 * <p><b>幂等核心</b>：{@code biz_key} 唯一键 {@code uk_notify_biz_key}。
 * Listener 重复触发同一事件（如 TaskCompletedEvent 重投）时，service 层用 existsByBizKey 短路；
 * 并发兜底由 DB 唯一约束完成。bizKey 约定格式 {@code <type>:<entityId>:<userId>}。
 *
 * <p>{@code readAt} 用 NULL 表示未读 —— 联合索引 (user_id, read_at, created_at) 支撑
 * 列表查询的"未读优先 + 按时间倒序"模式。
 */
@Entity
@Table(
    name = "notify_message",
    indexes = {
        @Index(name = "uk_notify_biz_key", columnList = "biz_key", unique = true),
        @Index(name = "idx_notify_user_unread", columnList = "user_id, read_at, created_at")
    }
)
public class NotifyMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", length = 32, nullable = false)
    private String type;

    @Column(name = "title", length = 128, nullable = false)
    private String title;

    @Column(name = "body", length = 512, nullable = false)
    private String body;

    @Column(name = "biz_key", length = 96, nullable = false, unique = true)
    private String bizKey;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public NotifyMessage() {}

    public NotifyMessage(Long userId, String type, String title, String body, String bizKey) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.bizKey = bizKey;
    }

    /** 标记已读（幂等：已读再调用不改 readAt）。 */
    public void markRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getBizKey() { return bizKey; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
