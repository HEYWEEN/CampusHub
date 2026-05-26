package com.campushub.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class TaskExtendDTO {

    @Min(1) @Max(120)
    private int additionalMinutes;

    public int getAdditionalMinutes() { return additionalMinutes; }
    public void setAdditionalMinutes(int v) { this.additionalMinutes = v; }
}
