package com.campushub.credit.entity;

/**
 * credit_record.direction：积分流水方向（schema.sql 注释「冻结/解冻/结算等，枚举由 Converter 映射」）。
 *
 * 用显式 code 落库（配合 {@link CreditDirectionConverter}），禁用 ordinal —— 枚举顺序变化会静默错位。
 */
public enum CreditDirection {
    /** 冻结：point_balance → point_frozen */
    FREEZE(0),
    /** 解冻：point_frozen → point_balance（取消时退回） */
    UNFREEZE(1),
    /** 结算：完成交易，积分计入收款方 point_balance */
    SETTLE(2),
    /** 扣减：信用分调整（差评/爽约/违规等，CRD-03 Strategy） */
    DEDUCT(3);

    private final int code;

    CreditDirection(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CreditDirection fromCode(int code) {
        for (CreditDirection d : values()) {
            if (d.code == code) return d;
        }
        throw new IllegalArgumentException("无效 credit_record.direction: " + code);
    }
}
