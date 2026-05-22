package com.campushub.auth.entity;

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
 * auth_verification：学生证实名认证申请（P3 schema.sql + §9.1）。
 *
 * 隐私字段全部 AES-256-GCM 密文（real_name / student_no / id_card）：
 *   - **业务表禁冗余真实姓名** — 即使要展示，也走 PublicUserVO，绝不下发到学生端
 *   - 仅管理端审批工具能解密
 *
 * 附件：仅存 SHA-256 JSON 列表（attachment_sha256），原图在对象存储；
 * 落库不存图本体（P3 §9.1）。
 */
@Entity
@Table(
    name = "auth_verification",
    indexes = {
        @Index(name = "idx_auth_verification_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_verification_status_created", columnList = "status,created_at")
    }
)
public class AuthVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Convert(converter = VerificationStatusConverter.class)
    @Column(name = "status", nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    /** AES-GCM(Base64) 真实姓名 */
    @Column(name = "real_name_cipher", length = 512)
    private String realNameCipher;

    /** AES-GCM(Base64) 学号 */
    @Column(name = "student_no_cipher", length = 512)
    private String studentNoCipher;

    /** AES-GCM(Base64) 身份证号（可选） */
    @Column(name = "id_card_cipher", length = 512)
    private String idCardCipher;

    /** 审批驳回原因（最长 500，APPROVED 时为 null） */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 证件图 SHA-256 列表 JSON，例：["abc...","def..."] */
    @Column(name = "attachment_sha256", columnDefinition = "TEXT")
    private String attachmentSha256Json;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AuthVerification() {}

    public AuthVerification(Long userId, String realNameCipher, String studentNoCipher,
                            String idCardCipher, String attachmentSha256Json) {
        this.userId = userId;
        this.realNameCipher = realNameCipher;
        this.studentNoCipher = studentNoCipher;
        this.idCardCipher = idCardCipher;
        this.attachmentSha256Json = attachmentSha256Json;
    }

    public void approve() {
        this.status = VerificationStatus.APPROVED;
        this.rejectReason = null;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = VerificationStatus.REJECTED;
        this.rejectReason = reason;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public VerificationStatus getStatus() { return status; }
    public String getRealNameCipher() { return realNameCipher; }
    public String getStudentNoCipher() { return studentNoCipher; }
    public String getIdCardCipher() { return idCardCipher; }
    public String getRejectReason() { return rejectReason; }
    public String getAttachmentSha256Json() { return attachmentSha256Json; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
