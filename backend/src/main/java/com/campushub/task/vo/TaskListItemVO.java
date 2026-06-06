package com.campushub.task.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;

import java.time.Instant;

public record TaskListItemVO(
        Long taskId,
        String title,
        String remark,
        TaskType taskType,
        TaskStatus status,
        int rewardPoint,
        Instant deadlineAt,
        String deliveryBuilding,
        PublicUserVO publisher,
        PublicUserVO assignee,
        Instant createdAt
) {
    /** 开放任务列表（推荐 / 检索无接单者语境）：assignee 置空。 */
    public static TaskListItemVO from(Task task, PublicUserVO publisher) {
        return from(task, publisher, null);
    }

    public static TaskListItemVO from(Task task, PublicUserVO publisher, PublicUserVO assignee) {
        return new TaskListItemVO(
                task.getId(),
                task.getTitle(),
                task.getRemark(),
                task.getTaskType(),
                task.getStatus(),
                task.getRewardPoint(),
                task.getDeadlineAt(),
                task.getDeliveryBuilding(),
                publisher,
                assignee,
                task.getCreatedAt()
        );
    }
}
