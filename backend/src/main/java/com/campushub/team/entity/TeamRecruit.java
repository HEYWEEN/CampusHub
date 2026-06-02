package com.campushub.team.entity;

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
    name = "team_recruit",
    indexes = {
        @Index(name = "idx_team_recruit_status_created", columnList = "status, created_at"),
        @Index(name = "idx_team_recruit_creator", columnList = "creator_id")
    }
)
public class TeamRecruit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    /** 技能标签，逗号分隔（1~5 个）。 */
    @Column(name = "skill_tags", length = 255)
    private String skillTags;

    @Column(name = "total_size", nullable = false)
    private int totalSize;

    @Column(name = "current_size", nullable = false)
    private int currentSize = 1;

    @Column(name = "status", nullable = false)
    private TeamRecruitStatus status = TeamRecruitStatus.RECRUITING;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public TeamRecruit() {}

    public TeamRecruit(Long creatorId, String title, String description, String skillTags, int totalSize) {
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.skillTags = skillTags;
        this.totalSize = totalSize;
        this.currentSize = 1;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    /** 成员 +1，达到总人数自动满员。 */
    public void joinOne() {
        this.currentSize += 1;
        if (this.currentSize >= this.totalSize) {
            this.status = TeamRecruitStatus.FULL;
        }
        touch();
    }

    public boolean isFull() {
        return currentSize >= totalSize;
    }

    public Long getId() { return id; }
    public Long getCreatorId() { return creatorId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSkillTags() { return skillTags; }
    public int getTotalSize() { return totalSize; }
    public int getCurrentSize() { return currentSize; }
    public TeamRecruitStatus getStatus() { return status; }
    public void setStatus(TeamRecruitStatus status) { this.status = status; touch(); }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
