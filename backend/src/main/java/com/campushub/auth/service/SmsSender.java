package com.campushub.auth.service;

import com.campushub.auth.entity.SmsScene;

/**
 * 短信发送渠道抽象。
 *
 * 当前阶段（P0）：由 {@link MockSmsSender} 实现，dev/local profile 下固定返回 123456。
 * 后续接入阿里云短信：新增 AliyunSmsSender impl，按 scene 映射到模板号；
 *   prod profile 装配 AliyunSmsSender，dev 仍走 MockSmsSender。
 */
public interface SmsSender {

    /**
     * 发送验证码。
     *
     * @param phone 明文手机号（仅渠道层短暂持有，不落库）
     * @param code  6 位验证码
     * @param scene 模板场景
     */
    void send(String phone, String code, SmsScene scene);

    /**
     * 由 SmsCodeService 调用生成验证码 —— 默认返回 6 位随机；
     * MockSmsSender 覆盖为固定 123456 便于联调。
     */
    default String nextCode() {
        int n = (int) (Math.random() * 1_000_000);
        return String.format("%06d", n);
    }
}
