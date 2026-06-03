package com.campushub.agent.vo;

import com.campushub.task.entity.TaskType;

/** 发单草稿（draft_task 工具产出，不落库；前端预填发布表单）。 */
public record TaskDraftVO(
        String title,
        TaskType taskType,
        int rewardPoint,
        String deadlineIso,
        String deliveryBuilding,
        String remark
) {}
