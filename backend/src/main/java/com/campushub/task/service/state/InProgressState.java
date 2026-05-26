package com.campushub.task.service.state;

import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;

import java.time.Instant;

public class InProgressState implements TaskState {

    @Override
    public void submitProof(Task task) {
        task.setStatus(TaskStatus.WAIT_CONFIRM);
    }

    @Override
    public void cancel(Task task) {
        task.setStatus(TaskStatus.CANCELED);
    }

    @Override
    public void expire(Task task) {
        task.setStatus(TaskStatus.EXPIRED);
    }

    @Override
    public void extend(Task task, Instant newDeadline) {
        task.setDeadlineAt(newDeadline);
    }
}
