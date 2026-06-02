package com.campushub.credit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** POST /api/credit/appeals — 发起差评申诉（F-CREDIT-05/06）。 */
public class CreditAppealCreateDTO {

    @NotNull
    private Long reviewId;

    @NotBlank
    @Size(max = 500, message = "申诉理由最多 500 字")
    private String reason;

    @Size(max = 5, message = "证据图最多 5 张")
    private List<String> evidenceUrls;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
}
