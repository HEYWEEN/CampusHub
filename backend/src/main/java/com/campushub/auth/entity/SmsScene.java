package com.campushub.auth.entity;

/**
 * 短信场景（与 OpenAPI SmsCodeSendRequest.scene 枚举对齐）。
 *   LOGIN_REGISTER  — 首次注册 / 忘记密码重置（P3 §2.5.1 验证码场景 1、2）
 *   RESET_PASSWORD  — 已登录用户改密
 *   SECURITY        — 敏感操作二次验证（后续扩展）
 */
public enum SmsScene {
    LOGIN_REGISTER,
    RESET_PASSWORD,
    SECURITY
}
