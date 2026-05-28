package com.campushub.notify.exception;

/**
 * notify 模块业务错误码（9xxx 段，P3 §2.4 错误码分段）。
 */
public final class NotifyErrorCode {

    private NotifyErrorCode() {}

    /** 消息不存在 — 404 */
    public static final int MESSAGE_NOT_FOUND = 9001;

    /** 越权访问他人的站内信 — 403 */
    public static final int MESSAGE_FORBIDDEN = 9002;
}
