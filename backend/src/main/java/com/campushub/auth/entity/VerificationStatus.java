package com.campushub.auth.entity;

/**
 * auth_verification.status 独立状态机（schema.sql 注释定义）。
 *
 * 注意：与 auth_user.verify_status 不同！
 *   auth_user.verify_status：用户全局认证摘要（GUEST/PENDING/APPROVED/REJECTED）
 *   auth_verification.status：单次提交的处理状态（PENDING/APPROVED/REJECTED）
 *
 * 关系：每次新提交 → 新增一行 status=PENDING，同时 auth_user.verify_status=PENDING；
 * 管理员审批通过 → 该行 status=APPROVED，auth_user.verify_status=APPROVED。
 */
public enum VerificationStatus {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);

    private final int code;

    VerificationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static VerificationStatus fromCode(int code) {
        for (VerificationStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 verification.status: " + code);
    }
}
