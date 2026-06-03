package com.campushub.agent.entity;

/** 消息角色（ORDINAL → INT，对齐项目枚举约定）。 */
public enum AgentRole {
    USER(0),
    ASSISTANT(1);

    private final int code;

    AgentRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
