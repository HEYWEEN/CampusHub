package com.campushub.auth.entity;

public enum Role {
    USER(0),
    ADMIN(1);

    private final int code;

    Role(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Role fromCode(int code) {
        for (Role r : values()) {
            if (r.code == code) return r;
        }
        throw new IllegalArgumentException("无效 auth_user.role: " + code);
    }
}
