package com.campushub.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户实名认证状态（与 auth_user.verify_status TINYINT 列对齐，P3 数据库 schema.sql）。
 *
 *   0 GUEST     — 仅手机号注册，未提交学生证
 *   1 PENDING   — 已提交学生证，待管理员审核
 *   2 APPROVED  — 审核通过（可发布任务等需要认证的操作）
 *   3 REJECTED  — 审核驳回，需重新提交
 *
 * JSON 序列化走小写（@JsonValue），与前端 'guest' | 'pending' | ... 字面量对齐
 * （schema_audit B-6 / B-7 修复，避免大小写不匹配）。
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

    /** Jackson 序列化为小写字符串：GUEST → "guest" */
    @JsonValue
    public String jsonValue() {
        return name().toLowerCase();
    }

    /** Jackson 反序列化：接受任意大小写 */
    @JsonCreator
    public static VerifyStatus fromJson(String s) {
        if (s == null) return null;
        return VerifyStatus.valueOf(s.toUpperCase());
    }

    public static VerifyStatus fromCode(int code) {
        for (VerifyStatus v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("无效的 verify_status: " + code);
    }
}
