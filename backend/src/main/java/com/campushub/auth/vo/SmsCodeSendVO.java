package com.campushub.auth.vo;

/**
 * POST /api/auth/sms-codes 响应。
 *
 * @param registered 该手机号是否已注册账号。
 *        前端验证码登录页据此决定是否展示「设置密码」——已注册账号无需再设。
 */
public record SmsCodeSendVO(boolean registered) {}
