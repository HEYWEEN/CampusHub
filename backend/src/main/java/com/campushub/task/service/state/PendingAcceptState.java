package com.campushub.task.service.state;

import com.campushub.task.dto.TaskUpdateDTO;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;

import java.time.Instant;

public class PendingAcceptState implements TaskState {

    @Override
    public void edit(Task task, TaskUpdateDTO dto) {
        if (dto.getDeliveryBuilding() != null) task.setDeliveryBuilding(dto.getDeliveryBuilding());
        if (dto.getDeadlineAt() != null) task.setDeadlineAt(dto.getDeadlineAt());
        if (dto.getRemark() != null) task.setRemark(dto.getRemark());
    }

    @Override
    public void accept(Task task, long accepterId) {
        task.setAssigneeId(accepterId);
        task.setStatus(TaskStatus.IN_PROGRESS);
    }

    @Override
    public void cancel(Task task) {
        task.setStatus(TaskStatus.CANCELED);
    }

    @Override
    public void expire(Task task) {
        task.setStatus(TaskStatus.EXPIRED);
    }
}
