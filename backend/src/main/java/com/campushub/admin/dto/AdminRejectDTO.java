package com.campushub.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PATCH /api/admin/verifications/{id} 驳回时的理由（≥5 字）。 */
public class AdminRejectDTO {

    @NotBlank
    @Size(min = 5, max = 200, message = "驳回理由需 5~200 字")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
