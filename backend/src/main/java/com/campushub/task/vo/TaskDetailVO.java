package com.campushub.task.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;

import java.time.Instant;
import java.util.List;

public record TaskDetailVO(
        Long taskId,
        String title,
        TaskType taskType,
        TaskStatus status,
        int rewardPoint,
        Instant deadlineAt,
        String pickupHint,
        String deliveryBuilding,
        String remark,
        PublicUserVO publisher,
        PublicUserVO assignee,
        List<String> attachmentUrls,
        Instant createdAt,
        boolean canAccept,
        boolean isPublisher
) {
    public static TaskDetailVO from(Task task, PublicUserVO publisher, PublicUserVO assignee,
                                     List<String> attachmentUrls, boolean canAccept, boolean isPublisher) {
        return new TaskDetailVO(
                task.getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getStatus(),
                task.getRewardPoint(),
                task.getDeadlineAt(),
                task.getPickupHint(),
                task.getDeliveryBuilding(),
                task.getRemark(),
                publisher,
                assignee,
                attachmentUrls,
                task.getCreatedAt(),
                canAccept,
                isPublisher
        );
    }
}
