package com.campushub.team.entity;

public enum TeamRecruitStatus {
    RECRUITING(0),
    FULL(1),
    CLOSED(2);

    private final int code;

    TeamRecruitStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TeamRecruitStatus fromCode(int code) {
        for (TeamRecruitStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 team_recruit.status: " + code);
    }
}
