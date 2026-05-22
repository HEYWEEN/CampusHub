package com.campushub.auth.repository;

import com.campushub.auth.entity.SmsCode;
import com.campushub.auth.entity.SmsScene;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsCodeRepository extends JpaRepository<SmsCode, Long> {

    /**
     * 取该 (phone, scene) 下最新一条未消费验证码 —— 用于 LoginByCode 校验时取最近发的。
     * 同时让重发覆盖旧码（旧的不主动删，靠 expireAt 自然失效 + 后续定时清理）。
     */
    Optional<SmsCode> findTopByPhoneHmacAndSceneAndConsumedAtIsNullOrderByCreatedAtDesc(
            String phoneHmac, SmsScene scene);
}
