package com.campushub.credit.entity;

public enum AppealStatus {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);

    private final int code;

    AppealStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AppealStatus fromCode(int code) {
        for (AppealStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 credit_appeal.status: " + code);
    }
}
