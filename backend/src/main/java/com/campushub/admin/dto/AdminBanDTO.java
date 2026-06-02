package com.campushub.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** PATCH /api/admin/users/{id}/ban —— 封禁/解封。 */
public class AdminBanDTO {

    @NotNull
    private Boolean banned;

    @Size(max = 200)
    private String reason;

    public Boolean getBanned() { return banned; }
    public void setBanned(Boolean banned) { this.banned = banned; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
