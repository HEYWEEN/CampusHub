package com.campushub.auth.controller;

import com.campushub.auth.dto.SmsCodeSendDTO;
import com.campushub.auth.service.SmsCodeService;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/auth/* —— 短信、登录、注册、JWT 续签、登出
 *
 * 本类目前只实现 AUTH-01；AUTH-02~04 由后续 commit 追加。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SmsCodeService smsCodeService;

    public AuthController(SmsCodeService smsCodeService) {
        this.smsCodeService = smsCodeService;
    }

    /**
     * POST /api/auth/sms-codes —— 发送短信验证码（F-AUTH-01）
     * 限流：手机 60s≤1 / 24h≤20，IP 60s≤5 / 24h≤200；超限 429。
     */
    @PostMapping("/sms-codes")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SmsCodeSendDTO dto,
                                          HttpServletRequest request) {
        String ip = IpUtil.resolve(request);
        smsCodeService.send(dto.getPhone(), dto.sceneOrDefault(), ip);
        return ApiResponse.success();
    }
}
