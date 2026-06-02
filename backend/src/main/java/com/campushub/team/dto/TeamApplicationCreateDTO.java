package com.campushub.team.dto;

import jakarta.validation.constraints.Size;

/** POST /api/team/recruits/{id}/applications — 申请加入（F-TEAM-02）。 */
public class TeamApplicationCreateDTO {

    @Size(max = 500, message = "申请留言最多 500 字")
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
