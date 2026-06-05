package com.campushub.trade.entity;

public enum TradeOfferStatus {
    PENDING(0),
    ACCEPTED(1),
    REJECTED(2),
    CANCELED(3);

    private final int code;

    TradeOfferStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TradeOfferStatus fromCode(int code) {
        for (TradeOfferStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("无效 trade_offer.status: " + code);
    }
}
