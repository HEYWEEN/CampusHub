package com.campushub.task.api;

import com.campushub.task.entity.TaskStatus;

public interface TaskApi {

    TaskStatus getTaskStatus(long taskId);

    Long getPublisherId(long taskId);

    int countInProgressBy(long userId);
}

