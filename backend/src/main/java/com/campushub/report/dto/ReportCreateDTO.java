package com.campushub.report.dto;

import com.campushub.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** POST /api/reports — 提交举报（F-REPORT-01）。 */
public class ReportCreateDTO {

    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private Long targetId;

    @NotBlank
    @Size(max = 64)
    private String reasonCategory;

    @Size(max = 1000, message = "举报描述最多 1000 字")
    private String description;

    @Size(max = 5, message = "证据图最多 5 张")
    private List<String> evidenceUrls;

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getReasonCategory() { return reasonCategory; }
    public void setReasonCategory(String reasonCategory) { this.reasonCategory = reasonCategory; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
}
