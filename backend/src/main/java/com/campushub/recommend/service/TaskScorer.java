package com.campushub.recommend.service;

import com.campushub.recommend.model.UserPreference;
import com.campushub.task.entity.Task;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 任务推荐打分器（规则加权，非 ML）。纯函数式，便于单测。
 *
 * <p>每个维度归一化到 [0,1] 后加权求和，总分 ∈ [0,1]。
 * 分两套权重：
 * <ul>
 *   <li><b>有偏好</b>：类型 0.30 + 位置 0.25 + 信用 0.15 + 悬赏 0.15 + 新鲜 0.10 + 紧急 0.05</li>
 *   <li><b>冷启动</b>（无历史，类型/位置维度恒 0）：把 0.55 权重重新分配给其余维度
 *       → 信用 0.40 + 悬赏 0.35 + 新鲜 0.15 + 紧急 0.10</li>
 * </ul>
 */
@Component
public class TaskScorer {

    // 有偏好权重
    static final double W_TYPE = 0.30;
    static final double W_LOCATION = 0.25;
    static final double W_CREDIT = 0.15;
    static final double W_REWARD = 0.15;
    static final double W_FRESH = 0.10;
    static final double W_URGENCY = 0.05;

    // 冷启动权重（无类型/位置信号，重分配）
    static final double C_CREDIT = 0.40;
    static final double C_REWARD = 0.35;
    static final double C_FRESH = 0.15;
    static final double C_URGENCY = 0.10;

    private static final double CREDIT_MAX = 120.0;     // 信用分上限
    private static final double FRESH_WINDOW_H = 7 * 24; // 新鲜度衰减窗口（7 天）
    private static final double URGENT_FLOOR_H = 12;     // ≤12h 视为最紧急
    private static final double URGENT_WINDOW_H = 48;    // 超过 48h 紧急度归 0

    /**
     * 给单个候选任务打分。
     *
     * @param t               候选任务
     * @param pref            当前用户偏好
     * @param publisherCredit 发布者信用分（[0,120]）
     * @param poolMaxReward   候选池内最大悬赏（用于归一化，≥1）
     * @param now             当前时刻
     * @return [0,1] 综合得分
     */
    public double score(Task t, UserPreference pref, int publisherCredit, double poolMaxReward, Instant now) {
        double credit = clamp01(publisherCredit / CREDIT_MAX);
        double reward = poolMaxReward > 0 ? clamp01(t.getRewardPoint() / poolMaxReward) : 0;
        double fresh = freshness(t.getCreatedAt(), now);
        double urgency = urgency(t.getDeadlineAt(), now);

        if (!pref.hasHistory()) {
            return C_CREDIT * credit + C_REWARD * reward + C_FRESH * fresh + C_URGENCY * urgency;
        }

        double type = pref.preferredType() != null && pref.preferredType() == t.getTaskType() ? 1 : 0;
        double location = pref.preferredBuilding() != null
                && pref.preferredBuilding().equals(t.getDeliveryBuilding()) ? 1 : 0;

        return W_TYPE * type + W_LOCATION * location
                + W_CREDIT * credit + W_REWARD * reward
                + W_FRESH * fresh + W_URGENCY * urgency;
    }

    /** 创建越近分越高；7 天线性衰减到 0。 */
    private double freshness(Instant createdAt, Instant now) {
        if (createdAt == null) return 0;
        double ageH = hoursBetween(createdAt, now);
        return clamp01(1 - ageH / FRESH_WINDOW_H);
    }

    /** 截止越近分越高；≤12h 满分，48h 外归 0。 */
    private double urgency(Instant deadlineAt, Instant now) {
        if (deadlineAt == null) return 0;
        double toDeadlineH = hoursBetween(now, deadlineAt);
        if (toDeadlineH <= URGENT_FLOOR_H) return toDeadlineH <= 0 ? 0 : 1; // 已过期不加分
        return clamp01(1 - (toDeadlineH - URGENT_FLOOR_H) / (URGENT_WINDOW_H - URGENT_FLOOR_H));
    }

    private static double hoursBetween(Instant from, Instant to) {
        return Duration.between(from, to).toMinutes() / 60.0;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
