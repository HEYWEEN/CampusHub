package com.campushub.auth.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码四档限流（OpenAPI /api/auth/sms-codes 第 31 行原文）：
 *   - 单手机号：60s ≤ 1 / 24h ≤ 20
 *   - 单 IP：    60s ≤ 5 / 24h ≤ 200
 *
 * 实现：内存滑动窗口。每个 key 维护一个 Deque&lt;时间戳&gt;，
 * 进入窗口前先剔除过期的时间戳（24h 之前），再分段计数。
 *
 * 单实例够用（P3 §3 Redis 默认禁用）；多实例部署时换 Redis ZSet。
 *
 * 线程安全：用 ConcurrentHashMap + synchronized 在 key 粒度上加锁。
 */
@Component
public class SmsRateLimiter {

    private static final Duration WIN_24H = Duration.ofHours(24);
    private static final Duration WIN_60S = Duration.ofSeconds(60);

    static final int PHONE_LIMIT_60S = 1;
    static final int PHONE_LIMIT_24H = 20;
    static final int IP_LIMIT_60S = 5;
    static final int IP_LIMIT_24H = 200;

    private final Map<String, Deque<Instant>> phoneTrack = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> ipTrack = new ConcurrentHashMap<>();

    /**
     * 同时检查手机号与 IP 的四档限制。任一档超限 → 抛 429。
     * 通过则把当前时间戳追加到两个轨迹里。
     */
    public void checkAndRecord(String phoneHmac, String ip) {
        Instant now = Instant.now();
        check(phoneTrack, phoneHmac, now, PHONE_LIMIT_60S, PHONE_LIMIT_24H, "手机号");
        check(ipTrack, ip, now, IP_LIMIT_60S, IP_LIMIT_24H, "IP");
        record(phoneTrack, phoneHmac, now);
        record(ipTrack, ip, now);
    }

    private void check(Map<String, Deque<Instant>> track, String key, Instant now,
                       int limit60s, int limit24h, String dimension) {
        Deque<Instant> deque = track.get(key);
        if (deque == null) return;
        synchronized (deque) {
            Instant cutoff24h = now.minus(WIN_24H);
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff24h)) {
                deque.pollFirst();
            }
            if (deque.size() >= limit24h) {
                throw new BizException(ResponseCode.RATE_LIMITED,
                        dimension + "短信发送已达 24 小时上限");
            }
            Instant cutoff60s = now.minus(WIN_60S);
            long recent = deque.stream().filter(t -> !t.isBefore(cutoff60s)).count();
            if (recent >= limit60s) {
                throw new BizException(ResponseCode.RATE_LIMITED,
                        dimension + "短信发送过于频繁，请稍后再试");
            }
        }
    }

    private void record(Map<String, Deque<Instant>> track, String key, Instant now) {
        Deque<Instant> deque = track.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(now);
        }
    }

    /**
     * 仅供单测在 @BeforeEach 中清空状态。
     * 生产代码不应调用 —— public 仅为跨包测试访问。
     */
    public void resetForTest() {
        phoneTrack.clear();
        ipTrack.clear();
    }
}
