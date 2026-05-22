package com.campushub.credit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * task_review：任务双向评分（P3 §1 schema.sql / P1 SRS FR-CRED-01）。
 *
 * <p>一个任务有两条评价（发布者→接单者、接单者→发布者）。唯一键
 * {@code uk_task_review_task_reviewer (task_id, reviewer_id)} 保证「一人对一任务只能评一次」。
 *
 * <p>跨模块只用逻辑外键（task_id 指向 task_order，但不建 JPA 关联，遵 P3 §1）。
 */
@Entity
@Table(
    name = "task_review",
    indexes = {
        @Index(name = "uk_task_review_task_reviewer", columnList = "task_id, reviewer_id", unique = true),
        @Index(name = "idx_task_review_reviewee", columnList = "reviewee_id, created_at")
    }
)
public class TaskReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", length = 500)
    private String comment;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TaskReview() {}

    public TaskReview(Long taskId, Long reviewerId, Long revieweeId, int rating, String comment) {
        this.taskId = taskId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public Long getReviewerId() { return reviewerId; }
    public Long getRevieweeId() { return revieweeId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
