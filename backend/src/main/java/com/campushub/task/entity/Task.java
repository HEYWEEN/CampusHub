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
    name = "task_order",
    indexes = {
        @Index(name = "idx_task_order_publisher_status", columnList = "publisher_id, status"),
        @Index(name = "idx_task_order_assignee_status", columnList = "assignee_id, status"),
        @Index(name = "idx_task_order_status_deadline", columnList = "status, deadline_at"),
        @Index(name = "idx_task_order_created_at", columnList = "created_at")
    }
)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Column(name = "reward_point", nullable = false)
    private int rewardPoint;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(name = "pickup_hint", length = 200, nullable = false)
    private String pickupHint;

    @Column(name = "delivery_building", length = 120, nullable = false)
    private String deliveryBuilding;

    @Column(name = "remark", length = 500)
    private String remark;

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

    public Task() {}

    public Task(Long publisherId, String title, TaskType taskType, TaskStatus status,
                int rewardPoint, Instant deadlineAt, String pickupHint,
                String deliveryBuilding, String remark) {
        this.publisherId = publisherId;
        this.title = title;
        this.taskType = taskType;
        this.status = status;
        this.rewardPoint = rewardPoint;
        this.deadlineAt = deadlineAt;
        this.pickupHint = pickupHint;
        this.deliveryBuilding = deliveryBuilding;
        this.remark = remark;
        this.creatorId = publisherId;
        this.updaterId = publisherId;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    // getters / setters

    public Long getId() { return id; }
    public Long getPublisherId() { return publisherId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; touch(); }
    public String getTitle() { return title; }
    public TaskType getTaskType() { return taskType; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) {
        if (!this.status.canTransitionTo(status)) {
            throw new IllegalArgumentException(
                "状态转换违法: " + this.status + " → " + status);
        }
        this.status = status;
        touch();
    }
    public int getRewardPoint() { return rewardPoint; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; touch(); }
    public String getPickupHint() { return pickupHint; }
    public String getDeliveryBuilding() { return deliveryBuilding; }
    public void setDeliveryBuilding(String v) { this.deliveryBuilding = v; touch(); }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; touch(); }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
