package com.campushub.trade.entity;

/** 砍价当前轮到谁出手。 */
public enum AwaitingParty {
    BUYER(0),
    SELLER(1);

    private final int code;

    AwaitingParty(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public AwaitingParty flip() {
        return this == BUYER ? SELLER : BUYER;
    }

    public static AwaitingParty fromCode(int code) {
        for (AwaitingParty p : values()) {
            if (p.code == code) return p;
        }
        throw new IllegalArgumentException("无效 trade_offer.awaiting_party: " + code);
    }
}
