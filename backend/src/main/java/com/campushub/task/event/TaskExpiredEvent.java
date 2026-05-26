package com.campushub.task.event;

public record TaskExpiredEvent(
        Long taskId,
        long version
) {}
