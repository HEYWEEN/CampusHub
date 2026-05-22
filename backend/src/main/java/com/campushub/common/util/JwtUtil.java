package com.campushub.common.util;

import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import com.campushub.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具：基于 jjwt 0.12.x。
 *
 * Token payload：
 *   sub: userId
 *   verifyStatus: GUEST / PENDING / APPROVED / REJECTED
 *   type: ACCESS / REFRESH
 *   iat / exp / iss
 */
@Component
public class JwtUtil {

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtUtil(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("campushub.jwt.secret 长度必须 ≥ 32 字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(long userId, String verifyStatus) {
        return generate(userId, verifyStatus, "ACCESS",
                (long) props.getAccessTtlMinutes() * 60 * 1000);
    }

    public String generateRefreshToken(long userId, String verifyStatus) {
        return generate(userId, verifyStatus, "REFRESH",
                (long) props.getRefreshTtlDays() * 24 * 60 * 60 * 1000);
    }

    private String generate(long userId, String verifyStatus, String type, long ttlMillis) {
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>();
        claims.put("verifyStatus", verifyStatus);
        claims.put("type", type);

        // jti 用于黑名单（logout / refresh-rotation）；UUID 短形式即可
        String jti = UUID.randomUUID().toString().replace("-", "");

        return Jwts.builder()
                .claims(claims)
                .id(jti)
                .subject(String.valueOf(userId))
                .issuer(props.getIssuer())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(signingKey)
                .compact();
    }

    /**
     * 解析并校验 token。无效 / 过期 抛 BizException(UNAUTHORIZED)。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "JWT 无效或已过期");
        }
    }

    public long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String getVerifyStatus(Claims claims) {
        Object v = claims.get("verifyStatus");
        return v == null ? "GUEST" : v.toString();
    }

    public String getType(Claims claims) {
        Object t = claims.get("type");
        return t == null ? "ACCESS" : t.toString();
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public Instant getExpiry(Claims claims) {
        return claims.getExpiration().toInstant();
    }
}
