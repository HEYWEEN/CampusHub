package com.campushub.user.vo;

/**
 * GET /api/users/me/stats —— 本人个人主页统计（真实跨模块聚合）。
 *
 * 与 {@link PublicUserStatsVO}（公开主页、受隐私开关影响、可为 null）不同：
 * 本人视角永远返回真实计数，不受隐私开关影响。
 */
public record MeStatsVO(
        long publishedCount,        // 我发布的任务（未删除）
        long acceptedCount,         // 我接过的任务（assigneeId = 我，未删除）
        long reviewsCount,          // 我收到的评价
        long publishedInProgress,   // 我发布的、进行中（IN_PROGRESS / WAIT_CONFIRM）
        long acceptedInProgress,    // 我接的、进行中
        Integer goodRate            // 好评率 %（rating≥4 占比）；无有效评价时 null
) {}
