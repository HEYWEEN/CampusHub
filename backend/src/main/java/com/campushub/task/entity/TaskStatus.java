package com.campushub.task.entity;

import java.util.Map;
import java.util.Set;

public enum TaskStatus {
    PENDING_ACCEPT(0),
    IN_PROGRESS(1),
    WAIT_CONFIRM(2),
    COMPLETED(3),
    CANCELED(4),
    EXPIRED(5);

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
        PENDING_ACCEPT, Set.of(IN_PROGRESS, CANCELED, EXPIRED),
        IN_PROGRESS,    Set.of(WAIT_CONFIRM, CANCELED, EXPIRED),
        WAIT_CONFIRM,   Set.of(COMPLETED, EXPIRED),
        COMPLETED,      Set.of(),
        CANCELED,       Set.of(),
        EXPIRED,        Set.of()
    );

    private final int code;

    TaskStatus(int code) { this.code = code; }
    public int getCode() { return code; }

    public boolean canTransitionTo(TaskStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static TaskStatus fromCode(int code) {
        for (TaskStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 task status: " + code);
    }

    @jakarta.persistence.Converter(autoApply = true)
    public static class TaskStatusConverter
            implements jakarta.persistence.AttributeConverter<TaskStatus, Integer> {
        @Override
        public Integer convertToDatabaseColumn(TaskStatus s) { return s.getCode(); }
        @Override
        public TaskStatus convertToEntityAttribute(Integer code) { return TaskStatus.fromCode(code); }
    }
}
