package com.campushub.common.util;

/**
 * 手机号脱敏：138****1234（前 3 后 4）。
 * 所有公开展示（包括 PublicUserVO、日志）的手机号必须先脱敏。
 */
public final class PhoneMaskUtil {

    private PhoneMaskUtil() {}

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
