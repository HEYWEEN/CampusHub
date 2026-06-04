package com.campushub.credit.repository;

import com.campushub.credit.entity.TaskReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskReviewRepository extends JpaRepository<TaskReview, Long> {

    /** 防重复评：该用户是否已对该任务评过。 */
    boolean existsByTaskIdAndReviewerId(long taskId, long reviewerId);

    /** 该任务的全部评价（最多 2 条，用于判断是否双方互评完成）。 */
    List<TaskReview> findByTaskId(long taskId);

    long countByTaskId(long taskId);

    /** 我收到的评价（申诉入口列表）。 */
    List<TaskReview> findByRevieweeIdOrderByCreatedAtDesc(long revieweeId);

    /** 我收到的评价数（个人主页统计 GET /api/users/me/stats）。 */
    long countByRevieweeId(long revieweeId);

    /** 有效评价总数（排除已作废）—— 好评率分母。 */
    long countByRevieweeIdAndVoidedFalse(long revieweeId);

    /** 好评数（rating ≥ minRating 且未作废）—— 好评率分子。 */
    long countByRevieweeIdAndRatingGreaterThanEqualAndVoidedFalse(long revieweeId, int minRating);
}
