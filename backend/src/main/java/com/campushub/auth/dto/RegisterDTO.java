package com.campushub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/auth/register  设置密码（OpenAPI RegisterRequest）。
 * 字段细颗粒度的"至少 1 字母 + 1 数字"由 PasswordPolicy 在 Service 层兜底，
 * 此处只做长度 + 非空 + 字符集校验。
 */
public class RegisterDTO {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Size(min = 4, max = 8, message = "验证码长度必须在 4-8 位之间")
    private String smsCode;

    @NotBlank
    @Size(min = 8, max = 32, message = "密码长度必须在 8-32 位之间")
    private String password;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSmsCode() { return smsCode; }
    public void setSmsCode(String smsCode) { this.smsCode = smsCode; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
