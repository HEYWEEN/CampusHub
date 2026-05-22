package com.campushub.auth.service;

import com.campushub.auth.entity.SmsCode;
import com.campushub.auth.entity.SmsScene;
import com.campushub.auth.exception.AuthErrorCode;
import com.campushub.auth.repository.SmsCodeRepository;
import com.campushub.common.exception.BizException;
import com.campushub.common.util.AesUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * 校验并消费短信验证码。
 *
 * 校验通过 → 立即标记 consumed_at = now，确保**一码一用**：
 *   - 防止注册接口和验证码登录接口同一个验证码连刷
 *   - 防止 RegisterRequest 重放
 *
 * 失败情形（统一抛 SMS_CODE_INVALID 401）：
 *   - 没有该 (phone, scene) 的未消费记录
 *   - 已过期
 *   - 验证码不匹配
 */
@Service
public class SmsCodeVerifier {

    private final SmsCodeRepository repo;
    private final AesUtil aes;

    public SmsCodeVerifier(SmsCodeRepository repo, AesUtil aes) {
        this.repo = repo;
        this.aes = aes;
    }

    @Transactional
    public void verifyAndConsume(String phone, String code, SmsScene scene) {
        String hmac = aes.hmacIndex(phone);
        Optional<SmsCode> opt = repo
                .findTopByPhoneHmacAndSceneAndConsumedAtIsNullOrderByCreatedAtDesc(hmac, scene);
        SmsCode record = opt.orElseThrow(() ->
                new BizException(AuthErrorCode.SMS_CODE_INVALID, "验证码无效或已过期", 401));

        Instant now = Instant.now();
        if (record.isExpired(now)) {
            throw new BizException(AuthErrorCode.SMS_CODE_INVALID, "验证码已过期", 401);
        }
        if (!record.getCode().equals(code)) {
            throw new BizException(AuthErrorCode.SMS_CODE_INVALID, "验证码不匹配", 401);
        }
        record.markConsumed(now);
        repo.save(record);
    }
}
