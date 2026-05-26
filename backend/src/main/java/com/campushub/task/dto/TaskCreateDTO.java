package com.campushub.task.dto;

import com.campushub.task.entity.TaskType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class TaskCreateDTO {

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotNull
    private TaskType taskType;

    @Min(1) @Max(500)
    private int rewardPoint;

    @NotNull
    @Future
    private Instant deadlineAt;

    @NotBlank
    @Size(max = 200)
    private String pickupHint;

    @NotBlank
    @Size(max = 120)
    private String deliveryBuilding;

    @Size(max = 500)
    private String remark;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public int getRewardPoint() { return rewardPoint; }
    public void setRewardPoint(int rewardPoint) { this.rewardPoint = rewardPoint; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }
    public String getPickupHint() { return pickupHint; }
    public void setPickupHint(String pickupHint) { this.pickupHint = pickupHint; }
    public String getDeliveryBuilding() { return deliveryBuilding; }
    public void setDeliveryBuilding(String deliveryBuilding) { this.deliveryBuilding = deliveryBuilding; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
