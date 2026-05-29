package com.campushub.auth.vo;

import com.campushub.auth.entity.VerificationStatus;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/auth/verifications/me 响应。
 * 隐私字段（realName/studentNo/idCard）**不下发**到学生端，仅提交时回显部分摘要。
 *
 * schema_audit A-12 修复后：attachmentUrls 替代原 attachmentSha256
 * （内容已从 sha 改为 URL 列表，对齐前端 ImageUploader 流程）。
 */
public class VerificationStatusVO {

    private Long id;
    private VerificationStatus status;
    private String rejectReason;
    private List<String> attachmentUrls;
    private Instant createdAt;
    private Instant updatedAt;

    public VerificationStatusVO() {}

    public VerificationStatusVO(Long id, VerificationStatus status, String rejectReason,
                                List<String> attachmentUrls, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.status = status;
        this.rejectReason = rejectReason;
        this.attachmentUrls = attachmentUrls;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public VerificationStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public List<String> getAttachmentUrls() { return attachmentUrls; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
