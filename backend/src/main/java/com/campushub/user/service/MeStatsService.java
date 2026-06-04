package com.campushub.user.service;

import com.campushub.credit.repository.TaskReviewRepository;
import com.campushub.task.repository.TaskRepository;
import com.campushub.user.vo.MeStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人主页三项统计的跨模块聚合（GET /api/users/me/stats）。
 *
 * 设计取舍：
 *   - 单独成 service，不污染 {@link UserService}（它同时实现跨模块 UserApi 契约，
 *     不宜反向依赖 task / credit 模块的 repo）。
 *   - 跨模块直接注入对方 repo —— 与 recommend / agent 模块一致的既有做法。
 */
@Service
public class MeStatsService {

    private final TaskRepository taskRepo;
    private final TaskReviewRepository reviewRepo;

    public MeStatsService(TaskRepository taskRepo, TaskReviewRepository reviewRepo) {
        this.taskRepo = taskRepo;
        this.reviewRepo = reviewRepo;
    }

    /** 好评门槛：rating ≥ 4 记为好评。 */
    private static final int GOOD_RATING = 4;

    @Transactional(readOnly = true)
    public MeStatsVO getMyStats(long userId) {
        long published = taskRepo.countByPublisherIdAndDeletedAtIsNull(userId);
        long accepted = taskRepo.countByAssigneeIdAndDeletedAtIsNull(userId);
        long reviews = reviewRepo.countByRevieweeId(userId);
        long publishedInProgress = taskRepo.countPublishedInProgress(userId);
        long acceptedInProgress = taskRepo.countAcceptedInProgress(userId);

        long validReviews = reviewRepo.countByRevieweeIdAndVoidedFalse(userId);
        Integer goodRate = validReviews == 0 ? null
                : Math.round(reviewRepo
                        .countByRevieweeIdAndRatingGreaterThanEqualAndVoidedFalse(userId, GOOD_RATING)
                        * 100f / validReviews);

        return new MeStatsVO(published, accepted, reviews,
                publishedInProgress, acceptedInProgress, goodRate);
    }
}
