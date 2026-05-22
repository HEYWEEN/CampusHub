package com.campushub.auth.entity;

import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.enums.VerifyStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * auth_user：鉴权用户主账户（P3 §1 schema.sql）。
 *
 * 隐私字段约束（P3 §9.1 强制）：
 *   - phone_hmac：HMAC-SHA256 索引，登录等值匹配
 *   - phone_cipher：AES-256-GCM 密文，仅审计/管理端可解
 *   - **永远不能**新增 phone_plain 字段或在 toString/log 中输出
 *
 * password_hash：BCrypt cost=10；验证码-only 用户（未走 register 流程）此字段为 NULL。
 *
 * 乐观锁：@Version 用于并发更新保护（例如 banned 标记 + 资料变更）。
 */
@Entity
@Table(
    name = "auth_user",
    indexes = {
        @Index(name = "uk_auth_user_phone_hmac", columnList = "phone_hmac", unique = true),
        @Index(name = "idx_auth_user_verify_status", columnList = "verify_status")
    }
)
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_hmac", length = 64, nullable = false, unique = true)
    private String phoneHmac;

    /** AES-GCM 密文（Base64）；管理端工具按 id 解密用 */
    @Column(name = "phone_cipher", length = 512, nullable = false)
    private String phoneCipher;

    /** BCrypt 哈希；验证码登录首次建账时为 null，走 register 后填入 */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Convert(converter = VerifyStatusConverter.class)
    @Column(name = "verify_status", nullable = false)
    private VerifyStatus verifyStatus = VerifyStatus.GUEST;

    @Column(name = "banned", nullable = false)
    private boolean banned = false;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AuthUser() {}

    public AuthUser(String phoneHmac, String phoneCipher) {
        this.phoneHmac = phoneHmac;
        this.phoneCipher = phoneCipher;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    public Long getId() { return id; }
    public String getPhoneHmac() { return phoneHmac; }
    public String getPhoneCipher() { return phoneCipher; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }
    public VerifyStatus getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(VerifyStatus verifyStatus) {
        this.verifyStatus = verifyStatus;
        this.updatedAt = Instant.now();
    }
    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
