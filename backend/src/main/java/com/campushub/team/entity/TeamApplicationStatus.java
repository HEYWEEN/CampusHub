package com.campushub.team.entity;

public enum TeamApplicationStatus {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);

    private final int code;

    TeamApplicationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TeamApplicationStatus fromCode(int code) {
        for (TeamApplicationStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 team_application.status: " + code);
    }
}
