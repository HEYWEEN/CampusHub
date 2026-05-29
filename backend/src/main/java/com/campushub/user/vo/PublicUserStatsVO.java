package com.campushub.user.vo;

import com.campushub.common.PublicUserVO;

/**
 * 公开用户主页 + 三项可隐藏统计。
 * 字段为 null 表示对方启用了对应隐私开关（前端按"已隐藏"渲染）。
 *
 * schema_audit A-10 修复：原本前端调 /api/users/{id}/public/stats 后端不存在。
 *
 * 当前 MVP：统计数据先返 null（=由对方"隐藏"开关兜底显示）；
 * 后续真实计数需要从 task_order / trade_order / task_review 跨模块聚合。
 */
public record PublicUserStatsVO(
        PublicUserVO user,
        Integer publishedCount,
        Integer acceptedCount,
        Integer reviewsCount
) {}
