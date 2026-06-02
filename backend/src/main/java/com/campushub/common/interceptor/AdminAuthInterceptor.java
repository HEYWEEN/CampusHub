package com.campushub.common.interceptor;

import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import com.campushub.common.util.CurrentUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端鉴权：仅拦 /api/admin/**，在 JwtAuthInterceptor 之后运行
 * （此时 CurrentUserHolder 已有 userId）。非 ADMIN → 403。
 *
 * <p>角色从 DB 实时查（JWT 不带 role；admin 端低频，查库可接受），
 * 这样改了 role 立即生效、无需等 token 过期。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AuthUserRepository userRepo;

    public AdminAuthInterceptor(AuthUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        long userId = CurrentUserHolder.getUserId();
        boolean isAdmin = userRepo.findById(userId).map(u -> u.isAdmin()).orElse(false);
        if (!isAdmin) {
            throw new BizException(ResponseCode.FORBIDDEN, "需要管理员权限");
        }
        return true;
    }
}
