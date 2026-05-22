package com.campushub.common.interceptor;

import com.campushub.common.util.TraceIdHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 每个请求生成或继承 traceId：
 *   1. 优先复用上游 X-Trace-Id（网关/前端传入）
 *   2. 否则随机生成 16 位
 *   3. 写入 MDC（日志自动带）+ TraceIdHolder（ApiResponse 读取）+ 响应头（前端可见）
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tid = request.getHeader(HEADER);
        if (tid == null || tid.isBlank()) {
            tid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        TraceIdHolder.set(tid);
        MDC.put(MDC_KEY, tid);
        response.setHeader(HEADER, tid);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TraceIdHolder.clear();
        MDC.remove(MDC_KEY);
    }
}
