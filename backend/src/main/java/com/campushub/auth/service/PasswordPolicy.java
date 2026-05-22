package com.campushub.auth.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;

/**
 * 密码策略校验（P3 §2.5.2）：
 *   - 长度 8 ~ 32
 *   - 至少 1 字母 + 1 数字
 *   - 允许的字符：ASCII 可打印（避免不可见字符引起的诡异 BCrypt 不一致）
 *
 * 不符合 → 抛 BizException(BAD_REQUEST 400, "密码强度不足")
 */
public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 32) {
            throw new BizException(ResponseCode.BAD_REQUEST, "密码长度必须在 8-32 位之间");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                throw new BizException(ResponseCode.BAD_REQUEST, "密码不允许包含非 ASCII 可见字符");
            }
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new BizException(ResponseCode.BAD_REQUEST, "密码必须同时包含字母和数字");
        }
    }
}
