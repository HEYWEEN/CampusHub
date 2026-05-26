package com.campushub.task.vo;

import java.time.Instant;

public record TaskExtendVO(
        Long taskId,
        Instant deadlineAt,
        int extendCount
) {}
