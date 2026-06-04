package com.campushub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/users/me/password —— 登录态设置 / 修改密码。
 * oldPassword 可空（验证码-only 用户首次设密码时不需要）；
 * newPassword 强度由 PasswordPolicy 在 service 层统一校验。
 */
public class ChangePasswordDTO {

    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 32, message = "密码长度必须在 8-32 位之间")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
