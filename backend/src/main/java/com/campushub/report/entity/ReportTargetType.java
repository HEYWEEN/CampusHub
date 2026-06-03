package com.campushub.report.entity;

/** 举报对象类型（ORDINAL → INT）。 */
public enum ReportTargetType {
    TASK(0),    // 任务
    TRADE(1),   // 二手商品/订单
    USER(2);    // 用户

    private final int code;

    ReportTargetType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ReportTargetType fromCode(int code) {
        for (ReportTargetType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("无效 report_case.target_type: " + code);
    }
}
