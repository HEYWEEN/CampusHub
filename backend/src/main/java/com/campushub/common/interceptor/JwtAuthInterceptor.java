package com.campushub.common.interceptor;

import com.campushub.auth.service.TokenBlacklist;
import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * JWT 鉴权拦截器：
 *   - 白名单（campushub.security.whitelist）放行
 *   - 非白名单必须带 Bearer Token，否则 401
 *   - Token 校验通过后 userId / verifyStatus 写入 CurrentUserHolder
 *
 * 注意：本拦截器不区分 ACCESS / REFRESH —— refresh 端点应只接收 REFRESH 类型，
 *      由 AuthService 自己校验 type 字段。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;
    private final List<String> whitelist;

    public JwtAuthInterceptor(
            JwtUtil jwtUtil,
            TokenBlacklist blacklist,
            @Value("${campushub.security.whitelist:/api/health,/api/auth/sms-codes,/api/auth/token,/api/auth/register}") String whitelistCsv
    ) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
        this.whitelist = Arrays.stream(whitelistCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // CORS 预检直接放行（WebMvcConfig 的 CORS 已处理 Header；这里防止 OPTIONS 走鉴权报错）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (isWhitelisted(path)) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "缺少 Authorization 头");
        }
        String token = auth.substring(BEARER_PREFIX.length()).trim();
        Claims claims = jwtUtil.parse(token);

        if (!"ACCESS".equals(jwtUtil.getType(claims))) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "需要 Access Token");
        }
        if (blacklist.isRevoked(jwtUtil.getJti(claims))) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "Token 已注销");
        }

        CurrentUserHolder.set(jwtUtil.getUserId(claims), jwtUtil.getVerifyStatus(claims));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : whitelist) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
