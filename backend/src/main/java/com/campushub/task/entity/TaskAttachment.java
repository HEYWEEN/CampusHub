package com.campushub.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(
    name = "task_attachment",
    indexes = {
        @Index(name = "idx_task_attachment_task_id", columnList = "task_id")
    }
)
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "url", length = 512, nullable = false)
    private String url;

    @Column(name = "kind", nullable = false)
    private int kind;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "updater_id")
    private Long updaterId;

    public TaskAttachment() {}

    public TaskAttachment(Long taskId, String url, int kind) {
        this.taskId = taskId;
        this.url = url;
        this.kind = kind;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public String getUrl() { return url; }
    public int getKind() { return kind; }
    public Instant getCreatedAt() { return createdAt; }
}
