package com.campushub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（绑定 application.properties 中 campushub.jwt.*）。
 *
 * 配置示例：
 *   campushub.jwt.secret=at-least-32-bytes-secret-key-here-please
 *   campushub.jwt.access-ttl-minutes=120
 *   campushub.jwt.refresh-ttl-days=14
 *   campushub.jwt.issuer=campushub
 */
@ConfigurationProperties(prefix = "campushub.jwt")
public class JwtProperties {

    /** HS256 密钥，长度需 ≥ 32 字节 */
    private String secret = "dev-default-secret-please-override-in-prod-32bytes!!";

    /** Access Token 有效期（分钟） */
    private int accessTtlMinutes = 120;

    /** Refresh Token 有效期（天） */
    private int refreshTtlDays = 14;

    /** 签发者标识 */
    private String issuer = "campushub";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public int getAccessTtlMinutes() { return accessTtlMinutes; }
    public void setAccessTtlMinutes(int accessTtlMinutes) { this.accessTtlMinutes = accessTtlMinutes; }
    public int getRefreshTtlDays() { return refreshTtlDays; }
    public void setRefreshTtlDays(int refreshTtlDays) { this.refreshTtlDays = refreshTtlDays; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
