package com.campushub.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** PATCH /api/admin/appeals/{id} —— 申诉裁决。 */
public class AdminAppealResolveDTO {

    /** true=通过（撤销差评），false=驳回。 */
    @NotNull
    private Boolean approve;

    @Size(max = 300)
    private String note;

    public Boolean getApprove() { return approve; }
    public void setApprove(Boolean approve) { this.approve = approve; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
