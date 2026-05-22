package com.campushub.common.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查端点：
 *   GET /api/health      白名单（任何人可访问，用于前端联调检测）
 *   GET /api/health/me   需登录（用于验证 JWT 拦截器是否生效）
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "campushub-backend");
        body.put("timestamp", Instant.now().toString());
        return ApiResponse.success(body);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", CurrentUserHolder.getUserIdOrNull());
        body.put("verifyStatus", CurrentUserHolder.getVerifyStatusOrNull());
        return ApiResponse.success(body);
    }
}
