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
}
