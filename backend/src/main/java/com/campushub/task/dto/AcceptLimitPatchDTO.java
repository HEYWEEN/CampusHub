package com.campushub.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class AcceptLimitPatchDTO {

    @Min(1) @Max(3)
    private int dailyAcceptLimit;

    public int getDailyAcceptLimit() { return dailyAcceptLimit; }
    public void setDailyAcceptLimit(int v) { this.dailyAcceptLimit = v; }
}
