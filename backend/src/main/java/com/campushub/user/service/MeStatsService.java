package com.campushub.user.service;

import com.campushub.common.PublicUserVO;
import com.campushub.credit.repository.TaskReviewRepository;
import com.campushub.task.repository.TaskRepository;
import com.campushub.user.vo.MeStatsVO;
import com.campushub.user.vo.PublicUserStatsVO;
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

    /**
     * 公开主页三项统计（GET /api/users/{id}/public/stats）。
     *
     * 与 {@link #getMyStats} 不同：这里按主页主人的隐私开关逐项 mask——
     * 被隐藏的项返回 null（前端渲染「已隐藏」），且不发起对应查询。
     * 「查看公开主页」本质是预览外人所见，故自己看自己也同样受隐私开关约束。
     */
    @Transactional(readOnly = true)
    public PublicUserStatsVO getPublicStats(PublicUserVO user, long userId,
                                            boolean hidePublished,
                                            boolean hideAccepted,
                                            boolean hideReviews) {
        Integer published = hidePublished ? null
                : (int) taskRepo.countByPublisherIdAndDeletedAtIsNull(userId);
        Integer accepted = hideAccepted ? null
                : (int) taskRepo.countByAssigneeIdAndDeletedAtIsNull(userId);
        Integer reviews = hideReviews ? null
                : (int) reviewRepo.countByRevieweeId(userId);
        return new PublicUserStatsVO(user, published, accepted, reviews);
    }
}
