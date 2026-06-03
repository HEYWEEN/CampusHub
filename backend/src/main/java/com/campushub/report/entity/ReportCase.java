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
 * 举报案件（F-REPORT-01）。多态目标：{@code targetType + targetId}。
 *
 * <p>{@code reportedUserId} 在提交时解析（USER=目标本人；TASK=发布者；TRADE=null，无 TradeApi），
 * 供裁决扣分使用。
 */
@Entity
@Table(
    name = "report_case",
    indexes = {
        @Index(name = "idx_report_case_status", columnList = "status, created_at"),
        @Index(name = "idx_report_case_reporter", columnList = "reporter_id, created_at"),
        @Index(name = "idx_report_case_target", columnList = "target_type, target_id")
    }
)
public class ReportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 被举报方用户 id；TRADE 目标无法解析时为 null。 */
    @Column(name = "reported_user_id")
    private Long reportedUserId;

    @Column(name = "reason_category", length = 64, nullable = false)
    private String reasonCategory;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "evidence_urls", length = 1000)
    private String evidenceUrls;

    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "handler_id")
    private Long handlerId;

    /** 最终裁决类型（已处理时非空）。 */
    @Column(name = "decision_type")
    private ReportDecisionType decisionType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ReportCase() {}

    public ReportCase(Long reporterId, ReportTargetType targetType, Long targetId,
                      Long reportedUserId, String reasonCategory, String description, String evidenceUrls) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reportedUserId = reportedUserId;
        this.reasonCategory = reasonCategory;
        this.description = description;
        this.evidenceUrls = evidenceUrls;
    }

    /** 裁决落地：更新状态 + 处理人 + 裁决类型。 */
    public void resolve(ReportStatus result, long handlerId, ReportDecisionType decisionType) {
        this.status = result;
        this.handlerId = handlerId;
        this.decisionType = decisionType;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getReporterId() { return reporterId; }
    public ReportTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Long getReportedUserId() { return reportedUserId; }
    public String getReasonCategory() { return reasonCategory; }
    public String getDescription() { return description; }
    public String getEvidenceUrls() { return evidenceUrls; }
    public ReportStatus getStatus() { return status; }
    public Long getHandlerId() { return handlerId; }
    public ReportDecisionType getDecisionType() { return decisionType; }
    public Instant getCreatedAt() { return createdAt; }
}
