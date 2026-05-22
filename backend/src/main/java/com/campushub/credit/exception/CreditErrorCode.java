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
}
