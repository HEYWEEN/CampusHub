package com.campushub.trade.entity;

public enum TradeItemStatus {
    ON_SALE(0),
    OFF_SALE(1),
    IN_TRADE(2);

    private final int code;

    TradeItemStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TradeItemStatus fromCode(int code) {
        for (TradeItemStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 trade_item.status: " + code);
    }
}
