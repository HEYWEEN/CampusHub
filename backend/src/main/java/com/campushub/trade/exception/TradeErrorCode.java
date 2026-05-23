package com.campushub.trade.exception;

/**
 * trade 模块错误码（5xxx 段，P4 §05）。
 */
public final class TradeErrorCode {

    private TradeErrorCode() {}

    public static final int ITEM_IMAGE_TOO_MANY = 5001;
    public static final int IMAGE_SIZE_EXCEEDED = 5002;
    public static final int ITEM_NOT_ON_SALE = 5003;
    public static final int ITEM_STATUS_INVALID = 5004;
    public static final int ORDER_ALREADY_CONFIRMED = 5005;
    public static final int ORDER_NOT_PARTICIPANT = 5006;
}
