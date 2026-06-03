package com.campushub.report.entity;

/**
 * 仲裁裁决类型（ORDINAL → INT）。简化版：
 * <ul>
 *   <li>{@code DISMISS} 举报不成立 → 驳回，不处罚</li>
 *   <li>{@code WARN} 举报成立 → 警告被举报方，不扣分</li>
 *   <li>{@code PENALIZE} 举报成立 → 扣被举报方信用分（需 reportedUserId 可解析）</li>
 * </ul>
 */
public enum ReportDecisionType {
    DISMISS(0),
    WARN(1),
    PENALIZE(2);

    private final int code;

    ReportDecisionType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ReportDecisionType fromCode(int code) {
        for (ReportDecisionType d : values()) {
            if (d.code == code) return d;
        }
        throw new IllegalArgumentException("无效 report decision_type: " + code);
    }
}
