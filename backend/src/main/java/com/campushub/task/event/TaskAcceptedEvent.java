package com.campushub.task.event;

public record TaskAcceptedEvent(
        Long taskId,
        Long publisherId,
        Long accepterId,
        Integer depositPoint,
        long version
) {}
