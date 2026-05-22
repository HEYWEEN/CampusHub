package com.campushub.auth.service;

import com.campushub.auth.entity.SmsScene;
import com.campushub.common.util.PhoneMaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 开发/测试环境短信发送：不真发，仅日志输出。验证码固定 123456，便于前后端联调。
 *
 * 生产环境（profile=prod）需要切换到真实渠道（如 AliyunSmsSender），
 * 本类用 @Profile("!prod") 排除 prod。
 */
@Component
@Profile("!prod")
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);
    private static final String FIXED_CODE = "123456";

    @Override
    public void send(String phone, String code, SmsScene scene) {
        log.info("[MockSms] 发送验证码 phone={} scene={} code={}",
                PhoneMaskUtil.mask(phone), scene, code);
    }

    /** dev 固定返回 123456，方便联调与单测断言 */
    @Override
    public String nextCode() {
        return FIXED_CODE;
    }
}
