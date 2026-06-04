package com.campushub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/auth/token/password  密码登录（OpenAPI LoginByPasswordRequest）。
 */
public class LoginByPasswordDTO {

    // 正常用户为 11 位手机号；额外放行字面量 "admin" 作为内置超级管理员账号的登录标识
    // （超管无法走验证码登录，故仅此密码登录入口需要放开）。
    @NotBlank
    @Pattern(regexp = "^(1[3-9]\\d{9}|admin)$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Size(min = 8, max = 32, message = "密码长度必须在 8-32 位之间")
    private String password;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
