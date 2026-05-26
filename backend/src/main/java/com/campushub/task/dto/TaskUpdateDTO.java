package com.campushub.task.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class TaskUpdateDTO {

    @Size(max = 120)
    private String deliveryBuilding;

    @Future
    private Instant deadlineAt;

    @Size(max = 500)
    private String remark;

    public String getDeliveryBuilding() { return deliveryBuilding; }
    public void setDeliveryBuilding(String v) { this.deliveryBuilding = v; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant v) { this.deadlineAt = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
}
