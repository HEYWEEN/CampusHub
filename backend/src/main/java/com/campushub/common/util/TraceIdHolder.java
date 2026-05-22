package com.campushub.common.util;

/**
 * 每次请求 traceId 的 ThreadLocal 容器。
 * 由 TraceIdInterceptor 写入，由 ApiResponse / 日志读取。
 */
public final class TraceIdHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceIdHolder() {}

    public static void set(String traceId) {
        HOLDER.set(traceId);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
