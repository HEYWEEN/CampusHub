package com.campushub.credit.vo;

import java.time.Instant;

/**
 * 积分流水条目（F-CREDIT-08，GET /api/credits/me/records）。字段与前端 types/credit.ts CreditRecord 对齐。
 *
 * @param direction FREEZE / UNFREEZE / SETTLE / DEDUCT
 */
public record CreditRecordVO(
        Long id,
        String direction,
        int delta,
        String reasonCode,
        String bizId,
        Instant createdAt) {
}
