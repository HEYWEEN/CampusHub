package com.campushub.credit.exception;

/**
 * credit 模块业务错误码（10xxx 段，P4 §05 错误码分段铁律）。
 * 与 ApiResponse.code 直接对接；HTTP 状态见各处抛出点。
 */
public final class CreditErrorCode {

    private CreditErrorCode() {}

    /** 可用积分不足以冻结/消费 — 422 */
    public static final int CREDIT_NOT_ENOUGH = 10001;

    /** 信用分过低，禁止该操作（&lt;60 禁发禁接） — 403 */
    public static final int SCORE_TOO_LOW = 10002;

    /** 账户不存在（一般走懒创建，不应抛出；仲裁等强一致场景用） — 404 */
    public static final int ACCOUNT_NOT_FOUND = 10003;

    /** 重复评价 / 不可重复评 — 409 */
    public static final int REVIEW_DUPLICATED = 10004;

    /** 只能申诉自己收到的评价 — 403 */
    public static final int APPEAL_NOT_REVIEWEE = 10005;

    /** 只有差评（评分 ≤ 2）可申诉 — 400 */
    public static final int APPEAL_NOT_NEGATIVE = 10006;

    /** 申诉窗口已过（差评 7 日内） — 422 */
    public static final int APPEAL_WINDOW_CLOSED = 10007;

    /** 7 日内申诉次数超限（≤ 3） — 422 */
    public static final int APPEAL_RATE_LIMIT = 10008;

    /** 申诉不存在 / 已处理 — 409 */
    public static final int APPEAL_NOT_PENDING = 10009;

    /** 仅已撤销的评价可从「我收到的评价」删除 — 422 */
    public static final int REVIEW_NOT_HIDABLE = 10010;

    /** 待处理的申诉不可删除（处理完才能删） — 409 */
    public static final int APPEAL_NOT_HIDABLE = 10011;
}
