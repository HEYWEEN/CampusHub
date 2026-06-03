package com.campushub.recommend.model;

import com.campushub.task.entity.TaskType;

/**
 * 用户偏好画像（从任务历史聚合而来，纯内存对象，不落库）。
 *
 * <p>{@code preferredType} / {@code preferredBuilding} 为该用户历史（发过 + 接过）任务里
 * 出现最多的类型 / 楼栋；无历史时均为 null（冷启动）。
 */
public record UserPreference(TaskType preferredType, String preferredBuilding) {

    public static final UserPreference EMPTY = new UserPreference(null, null);

    /** 是否有可用偏好；无则走冷启动打分。 */
    public boolean hasHistory() {
        return preferredType != null || preferredBuilding != null;
    }
}
