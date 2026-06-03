package com.campushub.report.entity;

/**
 * 举报案件状态。无 @Enumerated → 默认 ORDINAL 持久化为 INT（与项目其余枚举一致，
 * code 即 ordinal，迁移列用 INT 才能通过 ddl-auto=validate）。
 */
public enum ReportStatus {
    PENDING(0),     // 待处理
    RESOLVED(1),    // 已处理（举报成立：警告/扣分）
    DISMISSED(2);   // 已驳回（举报不成立）

    private final int code;

    ReportStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ReportStatus fromCode(int code) {
        for (ReportStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 report_case.status: " + code);
    }
}
