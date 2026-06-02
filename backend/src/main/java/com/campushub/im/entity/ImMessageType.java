package com.campushub.im.entity;

public enum ImMessageType {
    TEXT(0),
    IMAGE(1),
    SYSTEM(2);

    private final int code;

    ImMessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ImMessageType fromCode(int code) {
        for (ImMessageType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("无效 im_message.content_type: " + code);
    }
}
