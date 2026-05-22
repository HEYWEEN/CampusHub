package com.campushub.auth.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsRateLimiterTest {

    private SmsRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new SmsRateLimiter();
        limiter.resetForTest();
    }

    @Test
    void phone_60s_limit1_secondCallRejected() {
        limiter.checkAndRecord("phone-A", "ip-1");
        BizException ex = assertThrows(BizException.class,
                () -> limiter.checkAndRecord("phone-A", "ip-2"));
        assertEquals(ResponseCode.RATE_LIMITED.getCode(), ex.getCode());
        assertEquals(429, ex.getHttpStatus());
    }

    @Test
    void differentPhones_independent_neitherBlocked() {
        limiter.checkAndRecord("phone-A", "ip-1");
        // 不同手机号 + 不同 IP，互不影响
        assertDoesNotThrow(() -> limiter.checkAndRecord("phone-B", "ip-2"));
    }

    @Test
    void ip_60s_limit5_sixthCallRejected() {
        for (int i = 0; i < 5; i++) {
            limiter.checkAndRecord("phone-" + i, "ip-shared");
        }
        BizException ex = assertThrows(BizException.class,
                () -> limiter.checkAndRecord("phone-6", "ip-shared"));
        assertEquals(429, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("IP"), "应为 IP 限流而非手机号限流");
    }

    @Test
    void phoneLimitTriggeredBeforeIpLimit() {
        // 同手机同 IP，第一次过，第二次触发的应该是 phone 60s 限制（因为先 check phone）
        limiter.checkAndRecord("phone-A", "ip-1");
        BizException ex = assertThrows(BizException.class,
                () -> limiter.checkAndRecord("phone-A", "ip-1"));
        assertTrue(ex.getMessage().contains("手机号"),
                "应优先报手机号限流，错误信息: " + ex.getMessage());
    }
}
