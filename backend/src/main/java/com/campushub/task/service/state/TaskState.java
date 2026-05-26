package com.campushub.task.service.state;

import com.campushub.common.exception.BizException;
import com.campushub.task.dto.TaskUpdateDTO;
import com.campushub.task.entity.Task;
import com.campushub.task.exception.TaskErrorCode;

import java.time.Instant;

public interface TaskState {

    default void edit(Task task, TaskUpdateDTO dto) {
        throw new BizException(TaskErrorCode.STATE_VIOLATION, "当前状态不允许编辑", 409);
    }

    default void accept(Task task, long accepterId) {
        throw new BizException(TaskErrorCode.STATE_VIOLATION, "当前状态不允许接单", 409);
    }

    default void submitProof(Task task) {
        throw new BizException(TaskErrorCode.STATE_VIOLATION, "当前状态不允许上传凭证", 409);
    }

    default void confirm(Task task) {
        throw new BizException(TaskErrorCode.STATE_VIOLATION, "当前状态不允许确认", 409);
    }

    default void cancel(Task task) {
        throw new BizException(TaskErrorCode.STATE_VIOLATION, "当前状态不允许取消", 409);
    }

    default void expire(Task task) {
        throw new BizException(TaskErrorCode.STATE_VIOLATION, "当前状态不可超时", 409);
    }

    default void extend(Task task, Instant newDeadline) {
        throw new BizException(TaskErrorCode.STATE_VIOLATION, "当前状态不允许延长", 409);
    }
}
