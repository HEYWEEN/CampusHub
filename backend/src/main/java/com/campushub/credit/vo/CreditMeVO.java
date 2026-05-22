package com.campushub.credit.vo;

/**
 * 我的信用/积分总览（F-CREDIT-01，GET /api/credits/me）。字段与前端 types/credit.ts CreditMeVO 对齐。
 *
 * @param userId           用户 id
 * @param creditScore      信用分 0~120
 * @param pointBalance     可用积分
 * @param pointFrozen      冻结积分
 * @param dailyAcceptLimit 信用派生的每日接单上限（<60→0 禁接；<80→1；否则默认值，task 模块 TASK-08 可再调）
 * @param canPublish       是否可发布（creditScore ≥ 60）
 * @param canAccept        是否可接单（creditScore ≥ 60）
 */
public record CreditMeVO(
        Long userId,
        int creditScore,
        int pointBalance,
        int pointFrozen,
        int dailyAcceptLimit,
        boolean canPublish,
        boolean canAccept) {
}
