package com.campushub.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 仲裁裁决存档（F-REPORT-04）。一经写入仅追加、不更新不删除（应用层约束 + 文档约定）。
 */
@Entity
@Table(
    name = "report_decision",
    indexes = {
        @Index(name = "idx_report_decision_case", columnList = "case_id"),
        @Index(name = "idx_report_decision_admin", columnList = "admin_id, created_at")
    }
)
public class ReportDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "decision_type", nullable = false)
    private ReportDecisionType decisionType;

    /** 扣分（仅 PENALIZE 有意义，其余为 0）。 */
    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ReportDecision() {}

    public ReportDecision(Long caseId, Long adminId, ReportDecisionType decisionType,
                          int penaltyPoints, String reason) {
        this.caseId = caseId;
        this.adminId = adminId;
        this.decisionType = decisionType;
        this.penaltyPoints = penaltyPoints;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public Long getAdminId() { return adminId; }
    public ReportDecisionType getDecisionType() { return decisionType; }
    public int getPenaltyPoints() { return penaltyPoints; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
