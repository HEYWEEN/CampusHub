package com.campushub.trade.entity;

public enum PickupLocationType {
    EXACT_DORM(0),
    BUILDING_RANGE(1),
    MEETUP(2);

    private final int code;

    PickupLocationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static PickupLocationType fromCode(int code) {
        for (PickupLocationType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("无效 pickup_location_type: " + code);
    }
}
