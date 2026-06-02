package com.campushub.team.dto;

import jakarta.validation.constraints.NotNull;

/** PATCH /api/team/applications/{id} — 队长审核（F-TEAM-03）。 */
public class TeamApplicationReviewDTO {

    /** true=同意，false=拒绝。 */
    @NotNull
    private Boolean approve;

    public Boolean getApprove() { return approve; }
    public void setApprove(Boolean approve) { this.approve = approve; }
}
