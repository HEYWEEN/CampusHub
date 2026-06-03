package com.campushub.report.exception;

/** report 模块错误码（11xxx 段）。 */
public final class ReportErrorCode {
    private ReportErrorCode() {}

    public static final int SELF_REPORT = 11001;          // 不能举报自己 — 400
    public static final int DUPLICATE_PENDING = 11002;    // 同目标已有待处理举报 — 409
    public static final int CASE_NOT_PENDING = 11003;     // 案件已处理 — 409
    public static final int PENALIZE_TARGET_UNKNOWN = 11004; // 被举报方不可解析，无法扣分 — 409
    public static final int INVALID_PENALTY = 11005;      // PENALIZE 必须给正分 — 400
}
