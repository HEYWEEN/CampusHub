package com.campushub.common.util;

/**
 * 当前请求的登录上下文。JwtAuthInterceptor 写入，业务层读取。
 * 通过 ThreadLocal 实现，避免业务层方法签名都带 userId 参数。
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> VERIFY_STATUS = new ThreadLocal<>();

    private CurrentUserHolder() {}

    public static void set(long userId, String verifyStatus) {
        USER_ID.set(userId);
        VERIFY_STATUS.set(verifyStatus);
    }

    /** 未登录返回 null（白名单接口可能没有 userId） */
    public static Long getUserIdOrNull() {
        return USER_ID.get();
    }

    public static String getVerifyStatusOrNull() {
        return VERIFY_STATUS.get();
    }

    /** 必须登录的接口调用；未登录抛 NPE 之前请先在拦截器拒掉 */
    public static long getUserId() {
        Long uid = USER_ID.get();
        if (uid == null) {
            throw new IllegalStateException("当前请求未登录，调用 getUserId() 之前应已通过拦截器");
        }
        return uid;
    }

    public static void clear() {
        USER_ID.remove();
        VERIFY_STATUS.remove();
    }
}
