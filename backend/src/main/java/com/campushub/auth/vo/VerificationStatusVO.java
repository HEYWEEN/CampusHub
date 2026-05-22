package com.campushub.auth.vo;

import com.campushub.auth.entity.VerificationStatus;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/auth/verifications/me 响应。
 * 隐私字段（realName/studentNo/idCard）**不下发**到学生端，仅提交时回显部分摘要。
 */
public class VerificationStatusVO {

    private Long id;
    private VerificationStatus status;
    private String rejectReason;
    private List<String> attachmentSha256;
    private Instant createdAt;
    private Instant updatedAt;

    public VerificationStatusVO() {}

    public VerificationStatusVO(Long id, VerificationStatus status, String rejectReason,
                                List<String> attachmentSha256, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.status = status;
        this.rejectReason = rejectReason;
        this.attachmentSha256 = attachmentSha256;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public VerificationStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public List<String> getAttachmentSha256() { return attachmentSha256; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
