package com.campushub.credit.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.credit.entity.AppealStatus;
import com.campushub.credit.entity.CreditAppeal;
import com.campushub.credit.entity.TaskReview;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record CreditAppealVO(
        Long appealId,
        Long reviewId,
        Integer reviewRating,
        String reviewComment,
        String reason,
        List<String> evidenceUrls,
        AppealStatus status,
        String resolveNote,
        PublicUserVO appellant,
        Instant createdAt
) {
    public static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** review 可能为 null（极端：评价被删）；appellant 仅 admin 视图需要，自助视图传 null。 */
    public static CreditAppealVO from(CreditAppeal a, TaskReview review, PublicUserVO appellant) {
        return new CreditAppealVO(
                a.getId(),
                a.getReviewId(),
                review == null ? null : review.getRating(),
                review == null ? null : review.getComment(),
                a.getReason(),
                split(a.getEvidenceUrls()),
                a.getStatus(),
                a.getResolveNote(),
                appellant,
                a.getCreatedAt()
        );
    }
}
