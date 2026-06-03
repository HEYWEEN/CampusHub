package com.campushub.recommend.service;

import com.campushub.task.vo.TaskListItemVO;

import java.util.List;

/** 智能匹配 / 推荐（P2）。当前实现：任务推荐。 */
public interface RecommendService {

    /**
     * 为指定用户推荐待接单任务（规则加权打分降序）。
     *
     * @param userId 当前登录用户
     * @param limit  返回条数（调用方已 clamp）
     */
    List<TaskListItemVO> recommendTasks(long userId, int limit);
}
