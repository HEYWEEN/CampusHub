package com.campushub.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "team_application",
    indexes = {
        @Index(name = "idx_team_app_recruit_status", columnList = "recruit_id, status"),
        @Index(name = "idx_team_app_applicant", columnList = "applicant_id, created_at")
    }
)
public class TeamApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recruit_id", nullable = false)
    private Long recruitId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "status", nullable = false)
    private TeamApplicationStatus status = TeamApplicationStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TeamApplication() {}

    public TeamApplication(Long recruitId, Long applicantId, String message) {
        this.recruitId = recruitId;
        this.applicantId = applicantId;
        this.message = message;
    }

    public Long getId() { return id; }
    public Long getRecruitId() { return recruitId; }
    public Long getApplicantId() { return applicantId; }
    public String getMessage() { return message; }
    public TeamApplicationStatus getStatus() { return status; }
    public void setStatus(TeamApplicationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
