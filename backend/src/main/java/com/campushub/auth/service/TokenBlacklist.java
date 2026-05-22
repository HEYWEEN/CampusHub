package com.campushub.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 黑名单（按 jti 拒收）。
 *
 * 用途：
 *   - 用户主动 logout → 把当前 access + refresh 入黑
 *   - refresh-rotation → 旧 refresh 用过即入黑（防重放）
 *
 * 实现：内存 Map&lt;jti, expireAt&gt;。定时清理已过期 jti，避免内存膨胀。
 * 多实例部署：换 Redis SET key=BL:{jti} TTL=token 剩余有效期。
 */
@Component
public class TokenBlacklist {

    private final Map<String, Instant> entries = new ConcurrentHashMap<>();

    /**
     * 把 jti 加入黑名单，直到 expireAt（token 自身的 exp）。
     * 即使重复加入也无副作用（幂等）。
     */
    public void revoke(String jti, Instant expireAt) {
        if (jti == null || expireAt == null) return;
        if (expireAt.isAfter(Instant.now())) {
            entries.put(jti, expireAt);
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Instant exp = entries.get(jti);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            entries.remove(jti);
            return false;
        }
        return true;
    }

    /** 每 10 分钟清理一次过期条目，防止 jti 堆积 */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 10 * 60 * 1000L)
    public void purgeExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }

    /** 仅供单测重置 */
    public void resetForTest() {
        entries.clear();
    }

    int sizeForTest() {
        return entries.size();
    }
}
