package com.campushub.edu.entity;

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
    name = "edu_tutor_task",
    indexes = @Index(name = "idx_edu_tutor_task_publisher", columnList = "publisher_id, status")
)
public class EduTutorTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(name = "subject", length = 120, nullable = false)
    private String subject;

    @Column(name = "description", length = 2000, nullable = false)
    private String description;

    @Column(name = "reward_point", nullable = false)
    private int rewardPoint;

    @Column(name = "status", nullable = false)
    private int status = 0;

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

    public EduTutorTask() {}

    public EduTutorTask(Long publisherId, String subject, String description, int rewardPoint) {
        this.publisherId = publisherId;
        this.subject = subject;
        this.description = description;
        this.rewardPoint = rewardPoint;
        this.creatorId = publisherId;
        this.updaterId = publisherId;
    }

    public Long getId() { return id; }
    public Long getPublisherId() { return publisherId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public int getRewardPoint() { return rewardPoint; }
    public int getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
