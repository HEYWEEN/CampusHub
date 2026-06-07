package com.campushub.credit.entity;

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
    name = "credit_appeal",
    indexes = {
        @Index(name = "idx_credit_appeal_appellant", columnList = "appellant_id, created_at"),
        @Index(name = "idx_credit_appeal_review_status", columnList = "review_id, status"),
        @Index(name = "idx_credit_appeal_status", columnList = "status, created_at")
    }
)
public class CreditAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "appellant_id", nullable = false)
    private Long appellantId;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "evidence_urls", length = 1000)
    private String evidenceUrls;

    @Column(name = "status", nullable = false)
    private AppealStatus status = AppealStatus.PENDING;

    @Column(name = "resolver_id")
    private Long resolverId;

    @Column(name = "resolve_note", length = 500)
    private String resolveNote;

    /** 申诉人主动从「我的申诉」隐藏（仅已处理的申诉可隐藏）。 */
    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CreditAppeal() {}

    public CreditAppeal(Long reviewId, Long appellantId, String reason, String evidenceUrls) {
        this.reviewId = reviewId;
        this.appellantId = appellantId;
        this.reason = reason;
        this.evidenceUrls = evidenceUrls;
    }

    public void resolve(AppealStatus result, long resolverId, String note) {
        this.status = result;
        this.resolverId = resolverId;
        this.resolveNote = note;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public Long getAppellantId() { return appellantId; }
    public String getReason() { return reason; }
    public String getEvidenceUrls() { return evidenceUrls; }
    public AppealStatus getStatus() { return status; }
    public Long getResolverId() { return resolverId; }
    public String getResolveNote() { return resolveNote; }
    public boolean isHidden() { return hidden; }
    public void hide() { this.hidden = true; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
}
