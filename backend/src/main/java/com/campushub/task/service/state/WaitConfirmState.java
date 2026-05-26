package com.campushub.task.service.state;

import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;

public class WaitConfirmState implements TaskState {

    @Override
    public void confirm(Task task) {
        task.setStatus(TaskStatus.COMPLETED);
    }

    @Override
    public void expire(Task task) {
        task.setStatus(TaskStatus.EXPIRED);
    }
}
