package com.campushub.auth.exception;

/**
 * auth 模块业务错误码（2xxx 段，P3 §2.4）。
 * 与 ApiResponse.code 直接对接；HTTP 状态见各处抛出点。
 */
public final class AuthErrorCode {

    private AuthErrorCode() {}

    /** 验证码不存在 / 已过期 / 已被消费 / 不匹配 — 401 */
    public static final int SMS_CODE_INVALID = 2001;

    /** 密码错误 / 账号不存在 — 401（合并以避免账号枚举） */
    public static final int CREDENTIALS_INVALID = 2002;

    /** 账号锁定 / 封禁 — 403（与 OpenAPI AccountLocked 示例一致） */
    public static final int ACCOUNT_LOCKED = 2003;

    /** 手机号已注册（已设密）再次走 register — 409 */
    public static final int PHONE_ALREADY_REGISTERED = 2004;

    /** 仅密码登录但用户未设密（验证码-only 用户） — 401 */
    public static final int PASSWORD_NOT_SET = 2005;
}
