package com.campushub.auth.controller;

import com.campushub.auth.dto.LoginByCodeDTO;
import com.campushub.auth.dto.LoginByPasswordDTO;
import com.campushub.auth.dto.RegisterDTO;
import com.campushub.auth.dto.SmsCodeSendDTO;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.service.SmsCodeService;
import com.campushub.auth.vo.TokenPairVO;
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
 * 已实现：AUTH-01 / AUTH-02（登录/注册/密码登录）。
 * 待办：AUTH-03 学生证认证；AUTH-04 refresh + logout。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SmsCodeService smsCodeService;
    private final AuthService authService;

    public AuthController(SmsCodeService smsCodeService, AuthService authService) {
        this.smsCodeService = smsCodeService;
        this.authService = authService;
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

    /**
     * POST /api/auth/token —— 验证码登录或首次注册（F-AUTH-02）
     * 首次返回 verifyStatus=GUEST。
     */
    @PostMapping("/token")
    public ApiResponse<TokenPairVO> loginOrRegisterByCode(@Valid @RequestBody LoginByCodeDTO dto) {
        return ApiResponse.success(
                authService.loginOrRegisterByCode(dto.getPhone(), dto.getSmsCode())
        );
    }

    /**
     * POST /api/auth/register —— 验证码通过后设置密码
     * 已设密的手机号再次调用 → 409 PHONE_ALREADY_REGISTERED。
     */
    @PostMapping("/register")
    public ApiResponse<TokenPairVO> register(@Valid @RequestBody RegisterDTO dto) {
        return ApiResponse.success(
                authService.registerWithPassword(dto.getPhone(), dto.getSmsCode(), dto.getPassword())
        );
    }

    /**
     * POST /api/auth/token/password —— 密码日常登录（P3 §2.5.1）
     * 用户不存在或密码错误统一 401，避免账号枚举。
     */
    @PostMapping("/token/password")
    public ApiResponse<TokenPairVO> loginByPassword(@Valid @RequestBody LoginByPasswordDTO dto) {
        return ApiResponse.success(
                authService.loginByPassword(dto.getPhone(), dto.getPassword())
        );
    }
}
