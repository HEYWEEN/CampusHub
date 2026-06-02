package com.campushub.credit.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.credit.entity.TaskReview;

import java.time.Instant;

/** 我收到的评价（申诉入口列表）。 */
public record ReceivedReviewVO(
        Long reviewId,
        Long taskId,
        PublicUserVO reviewer,
        int rating,
        String comment,
        boolean voided,
        boolean underAppeal,
        boolean appealable,
        Instant createdAt
) {
    public static ReceivedReviewVO from(TaskReview r, PublicUserVO reviewer,
                                        boolean underAppeal, boolean appealable) {
        return new ReceivedReviewVO(
                r.getId(), r.getTaskId(), reviewer, r.getRating(),
                r.isVoided() ? "该评价已撤销" : r.getComment(),
                r.isVoided(), underAppeal, appealable, r.getCreatedAt());
    }
}
