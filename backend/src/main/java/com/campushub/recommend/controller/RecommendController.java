package com.campushub.recommend.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.recommend.service.RecommendService;
import com.campushub.task.vo.TaskListItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能匹配 / 推荐（P2）。在 /api/** 下，经 JwtAuthInterceptor 鉴权 → 必须登录。
 */
@RestController
public class RecommendController {

    /** 推荐条数边界：默认 8，最多 20。 */
    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/api/recommend/tasks")
    public ApiResponse<List<TaskListItemVO>> recommendTasks(
            @RequestParam(value = "limit", required = false, defaultValue = "8") int limit) {
        long userId = CurrentUserHolder.getUserId();
        int safe = Math.max(1, Math.min(limit, MAX_LIMIT));
        return ApiResponse.success(recommendService.recommendTasks(userId, safe));
    }
}
