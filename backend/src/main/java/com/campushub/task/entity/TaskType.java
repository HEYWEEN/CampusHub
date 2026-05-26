package com.campushub.task.entity;

public enum TaskType {
    ERRAND(0),
    MUTUAL_HELP(1),
    TUTOR(2);

    private final int code;

    TaskType(int code) { this.code = code; }
    public int getCode() { return code; }

    public static TaskType fromCode(int code) {
        for (TaskType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("无效 task_type: " + code);
    }

    @jakarta.persistence.Converter(autoApply = true)
    public static class TaskTypeConverter
            implements jakarta.persistence.AttributeConverter<TaskType, Integer> {
        @Override
        public Integer convertToDatabaseColumn(TaskType t) { return t.getCode(); }
        @Override
        public TaskType convertToEntityAttribute(Integer code) { return TaskType.fromCode(code); }
    }
}
