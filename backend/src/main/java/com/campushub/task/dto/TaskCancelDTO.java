package com.campushub.task.dto;

import jakarta.validation.constraints.Size;

public class TaskCancelDTO {

    @Size(max = 500)
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
}
