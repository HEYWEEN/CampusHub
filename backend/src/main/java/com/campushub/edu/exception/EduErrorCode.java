package com.campushub.edu.exception;

/**
 * edu 模块错误码（6xxx 段，P4 §05）。
 */
public final class EduErrorCode {

    private EduErrorCode() {}

    public static final int FORBIDDEN_WORD_HIT = 6001;
    public static final int COOLDOWN_ACTIVE = 6002;
}
