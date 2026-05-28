package com.campushub.task.event;

public record TaskExpiredEvent(
        Long taskId,
        Long publisherId,
        Long accepterId,
        long version
) {}
