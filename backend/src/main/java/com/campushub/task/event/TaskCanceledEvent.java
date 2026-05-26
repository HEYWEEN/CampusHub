package com.campushub.task.event;

public record TaskCanceledEvent(
        Long taskId,
        Long publisherId,
        Long accepterId,
        Integer rewardPoint,
        Integer depositPoint,
        long version
) {}
