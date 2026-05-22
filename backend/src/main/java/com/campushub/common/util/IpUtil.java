package com.campushub.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 取请求真实客户端 IP。
 * 优先 X-Forwarded-For 首段（反向代理常用）；回退 RemoteAddr。
 *
 * 注意：X-Forwarded-For 可被客户端伪造，仅当部署在可信反代之后才有意义。
 * 限流场景下被伪造的影响：攻击者可以快速换 IP 绕过 IP 档限流，但仍受手机号档约束。
 */
public final class IpUtil {

    private IpUtil() {}

    public static String resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            String first = comma >= 0 ? xff.substring(0, comma) : xff;
            return first.trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
