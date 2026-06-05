package com.campushub.auth.service;

import com.campushub.auth.entity.SmsCode;
import com.campushub.auth.entity.SmsScene;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.repository.SmsCodeRepository;
import com.campushub.common.util.AesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 短信验证码核心服务：限流 → 生成 → 落库 → 发送。
 *
 * 调用顺序刻意保留：
 *   1. 限流 check（最先抛 429，避免 DB 写入浪费）
 *   2. 生成 6 位 code（dev 固定 123456）
 *   3. 用 phone 的 HMAC 索引存 SmsCode（**不存明文手机号**）
 *   4. 调 SmsSender 真发
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final Duration TTL = Duration.ofMinutes(5);

    private final SmsCodeRepository repo;
    private final SmsRateLimiter limiter;
    private final SmsSender sender;
    private final AesUtil aes;
    private final AuthUserRepository userRepo;

    public SmsCodeService(SmsCodeRepository repo, SmsRateLimiter limiter,
                          SmsSender sender, AesUtil aes, AuthUserRepository userRepo) {
        this.repo = repo;
        this.limiter = limiter;
        this.sender = sender;
        this.aes = aes;
        this.userRepo = userRepo;
    }

    /**
     * 发送验证码，并返回该手机号是否已注册。
     * 前端据此决定验证码登录页是否展示「设置密码」（已注册账号无需再设）。
     *
     * @return true=该手机号已有账号；false=未注册（验证码登录时会自动注册）
     */
    @Transactional
    public boolean send(String phone, SmsScene scene, String ip) {
        String phoneHmac = aes.hmacIndex(phone);
        limiter.checkAndRecord(phoneHmac, ip);

        String code = sender.nextCode();
        Instant now = Instant.now();
        SmsCode record = new SmsCode(phoneHmac, code, scene, now.plus(TTL));
        repo.save(record);

        sender.send(phone, code, scene);
        log.debug("已为 phoneHmac={} scene={} 写入新验证码，TTL=5min", phoneHmac, scene);
        return userRepo.existsByPhoneHmac(phoneHmac);
    }
}
