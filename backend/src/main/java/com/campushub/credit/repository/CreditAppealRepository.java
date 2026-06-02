package com.campushub.credit.repository;

import com.campushub.credit.entity.AppealStatus;
import com.campushub.credit.entity.CreditAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CreditAppealRepository extends JpaRepository<CreditAppeal, Long> {

    /** 7 日内限频：同一申诉人最近申诉数。 */
    long countByAppellantIdAndCreatedAtAfter(Long appellantId, Instant after);

    /** 打码用：某评价是否有进行中的申诉。 */
    boolean existsByReviewIdAndStatus(Long reviewId, AppealStatus status);

    List<CreditAppeal> findByAppellantIdOrderByCreatedAtDesc(Long appellantId);

    /** admin 待审队列。 */
    List<CreditAppeal> findByStatusOrderByCreatedAtAsc(AppealStatus status);
}
