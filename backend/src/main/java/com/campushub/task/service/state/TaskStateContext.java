package com.campushub.task.service.state;

import com.campushub.task.dto.TaskUpdateDTO;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;

import java.time.Instant;
import java.util.Map;

public class TaskStateContext {

    private final Task task;
    private final TaskState state;

    private static final Map<TaskStatus, TaskState> STATES = Map.of(
        TaskStatus.PENDING_ACCEPT, new PendingAcceptState(),
        TaskStatus.IN_PROGRESS,    new InProgressState(),
        TaskStatus.WAIT_CONFIRM,   new WaitConfirmState(),
        TaskStatus.COMPLETED,      new CompletedState(),
        TaskStatus.CANCELED,       new CanceledState(),
        TaskStatus.EXPIRED,        new ExpiredState()
    );

    public TaskStateContext(Task task) {
        this.task = task;
        this.state = STATES.getOrDefault(task.getStatus(), new ExpiredState());
    }

    public void edit(TaskUpdateDTO dto)       { state.edit(task, dto); }
    public void accept(long accepterId)       { state.accept(task, accepterId); }
    public void submitProof()                 { state.submitProof(task); }
    public void confirm()                     { state.confirm(task); }
    public void cancel()                      { state.cancel(task); }
    public void expire()                      { state.expire(task); }
    public void extend(Instant newDeadline)   { state.extend(task, newDeadline); }
}
