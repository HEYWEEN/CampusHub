package com.campushub.auth.vo;

import com.campushub.common.enums.VerifyStatus;

import java.time.Instant;

/**
 * 登录/注册响应（OpenAPI TokenPair schema 对齐）。
 */
public class TokenPairVO {

    private String accessToken;
    private String refreshToken;
    private Instant accessExpiresAt;
    private Instant refreshExpiresAt;
    private VerifyStatus verifyStatus;

    public TokenPairVO() {}

    public TokenPairVO(String accessToken, String refreshToken,
                       Instant accessExpiresAt, Instant refreshExpiresAt,
                       VerifyStatus verifyStatus) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.verifyStatus = verifyStatus;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getAccessExpiresAt() { return accessExpiresAt; }
    public Instant getRefreshExpiresAt() { return refreshExpiresAt; }
    public VerifyStatus getVerifyStatus() { return verifyStatus; }
}
