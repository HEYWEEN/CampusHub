package com.campushub.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "task_extend_log")
public class TaskExtendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "old_deadline_at", nullable = false)
    private Instant oldDeadlineAt;

    @Column(name = "new_deadline_at", nullable = false)
    private Instant newDeadlineAt;

    @Column(name = "extend_count", nullable = false)
    private int extendCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public TaskExtendLog() {}

    public TaskExtendLog(Long taskId, Instant oldDeadlineAt, Instant newDeadlineAt, int extendCount) {
        this.taskId = taskId;
        this.oldDeadlineAt = oldDeadlineAt;
        this.newDeadlineAt = newDeadlineAt;
        this.extendCount = extendCount;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public Instant getOldDeadlineAt() { return oldDeadlineAt; }
    public Instant getNewDeadlineAt() { return newDeadlineAt; }
    public int getExtendCount() { return extendCount; }
    public Instant getCreatedAt() { return createdAt; }
}
