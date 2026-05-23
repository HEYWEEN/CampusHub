package com.campushub.trade.entity;

public enum TradeOrderStatus {
    IN_TRADE(0),
    BUYER_CONFIRMED(1),
    SELLER_CONFIRMED(2),
    COMPLETED(3),
    CANCELED(4);

    private final int code;

    TradeOrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TradeOrderStatus fromCode(int code) {
        for (TradeOrderStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 trade_order.status: " + code);
    }
}
