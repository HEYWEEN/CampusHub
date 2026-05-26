package com.campushub.task.vo;

import com.campushub.task.entity.TaskStatus;

import java.util.List;

public record TaskProofVO(
        Long taskId,
        TaskStatus status,
        String proofText,
        List<String> proofImages
) {}
