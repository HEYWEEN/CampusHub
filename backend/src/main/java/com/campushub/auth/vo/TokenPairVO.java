package com.campushub.auth.vo;

import com.campushub.common.enums.VerifyStatus;

import java.time.Instant;

/**
 * 登录/注册响应（OpenAPI TokenPair schema 对齐）。
 *
 * userId 保持 Long → JSON number；前端 type 写的是 string，
 * 但 zustand 不强校验运行时类型，能跑。真正 ID 类型统一留到 P3 全局 ToStringSerializer。
 * （schema_audit B-6）
 */
public class TokenPairVO {

    private Long userId;

    private String accessToken;
    private String refreshToken;
    private Instant accessExpiresAt;
    private Instant refreshExpiresAt;
    private VerifyStatus verifyStatus;

    public TokenPairVO() {}

    public TokenPairVO(Long userId, String accessToken, String refreshToken,
                       Instant accessExpiresAt, Instant refreshExpiresAt,
                       VerifyStatus verifyStatus) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.verifyStatus = verifyStatus;
    }

    public Long getUserId() { return userId; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getAccessExpiresAt() { return accessExpiresAt; }
    public Instant getRefreshExpiresAt() { return refreshExpiresAt; }
    public VerifyStatus getVerifyStatus() { return verifyStatus; }
}
