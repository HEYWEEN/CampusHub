package com.campushub.common.enums;

/**
 * 用户实名认证状态（与 auth_user.verify_status TINYINT 列对齐，P3 数据库 schema.sql）。
 *
 *   0 GUEST     — 仅手机号注册，未提交学生证
 *   1 PENDING   — 已提交学生证，待管理员审核
 *   2 APPROVED  — 审核通过（可发布任务等需要认证的操作）
 *   3 REJECTED  — 审核驳回，需重新提交
 */
public enum VerifyStatus {
    GUEST(0),
    PENDING(1),
    APPROVED(2),
    REJECTED(3);

    private final int code;

    VerifyStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static VerifyStatus fromCode(int code) {
        for (VerifyStatus v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("无效的 verify_status: " + code);
    }
}
