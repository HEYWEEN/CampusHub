package com.campushub.task.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;

import java.time.Instant;

public record TaskListItemVO(
        Long taskId,
        String title,
        TaskType taskType,
        TaskStatus status,
        int rewardPoint,
        Instant deadlineAt,
        String deliveryBuilding,
        PublicUserVO publisher,
        Instant createdAt
) {
    public static TaskListItemVO from(Task task, PublicUserVO publisher) {
        return new TaskListItemVO(
                task.getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getStatus(),
                task.getRewardPoint(),
                task.getDeadlineAt(),
                task.getDeliveryBuilding(),
                publisher,
                task.getCreatedAt()
        );
    }
}
