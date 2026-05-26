package com.campushub.task.dto;

import jakarta.validation.constraints.Min;

public class TaskAcceptDTO {

    @Min(0)
    private int version;

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
