package com.campushub.task.event;

public record TaskCompletedEvent(
        Long taskId,
        Long publisherId,
        Long accepterId,
        Integer rewardPoint,
        long version
) {}
