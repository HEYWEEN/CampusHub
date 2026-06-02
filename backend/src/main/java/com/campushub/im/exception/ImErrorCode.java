package com.campushub.im.exception;

/**
 * im 模块业务错误码（8xxx 段；2/4/5/6/7/9/10xxx 已被其它模块占用）。
 */
public final class ImErrorCode {

    private ImErrorCode() {}

    /** 非会话参与者 — 403 */
    public static final int NOT_PARTICIPANT = 8001;

    /** 不能和自己私信 — 400 */
    public static final int CANNOT_CHAT_SELF = 8002;

    /** 对方用户不存在 — 404 */
    public static final int PEER_NOT_FOUND = 8003;
}
