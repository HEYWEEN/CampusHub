package com.campushub.common.util;

import com.campushub.common.exception.BizException;
import com.campushub.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil newUtil(int accessTtlMin) {
        JwtProperties p = new JwtProperties();
        p.setSecret("test-secret-32bytes-test-secret-32bytes!!");
        p.setAccessTtlMinutes(accessTtlMin);
        p.setRefreshTtlDays(7);
        p.setIssuer("test");
        return new JwtUtil(p);
    }

    @Test
    void generateAndParseAccessToken() {
        JwtUtil util = newUtil(60);
        String token = util.generateAccessToken(42L, "APPROVED");

        Claims claims = util.parse(token);
        assertEquals(42L, util.getUserId(claims));
        assertEquals("APPROVED", util.getVerifyStatus(claims));
        assertEquals("ACCESS", util.getType(claims));
    }

    @Test
    void refreshTokenCarriesRefreshType() {
        JwtUtil util = newUtil(60);
        String token = util.generateRefreshToken(7L, "GUEST");
        Claims claims = util.parse(token);
        assertEquals("REFRESH", util.getType(claims));
    }

    @Test
    void expiredTokenThrowsBiz401() {
        JwtUtil util = newUtil(0); // 立即过期
        String token = util.generateAccessToken(1L, "GUEST");
        BizException ex = assertThrows(BizException.class, () -> util.parse(token));
        assertEquals(401, ex.getHttpStatus());
    }

    @Test
    void tamperedTokenThrowsBiz401() {
        JwtUtil util = newUtil(60);
        String token = util.generateAccessToken(1L, "GUEST");
        String tampered = token.substring(0, token.length() - 4) + "abcd";
        assertThrows(BizException.class, () -> util.parse(tampered));
    }

    @Test
    void shortSecretRejected() {
        JwtProperties p = new JwtProperties();
        p.setSecret("too-short");
        assertThrows(IllegalStateException.class, () -> new JwtUtil(p));
    }
}
