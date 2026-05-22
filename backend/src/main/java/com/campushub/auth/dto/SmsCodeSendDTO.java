package com.campushub.auth.dto;

import com.campushub.auth.entity.SmsScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST /api/auth/sms-codes 请求体（与 OpenAPI SmsCodeSendRequest 对齐）。
 */
public class SmsCodeSendDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 不传默认 LOGIN_REGISTER */
    private SmsScene scene;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public SmsScene getScene() { return scene; }
    public void setScene(SmsScene scene) { this.scene = scene; }

    public SmsScene sceneOrDefault() {
        return scene == null ? SmsScene.LOGIN_REGISTER : scene;
    }
}
