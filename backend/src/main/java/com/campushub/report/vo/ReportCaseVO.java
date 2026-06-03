package com.campushub.report.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.report.entity.ReportCase;
import com.campushub.report.entity.ReportDecisionType;
import com.campushub.report.entity.ReportStatus;
import com.campushub.report.entity.ReportTargetType;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * 举报案件视图。{@code reporter} 仅 admin 队列视图填充；用户「我的举报」视图为 null。
 */
public record ReportCaseVO(
        Long caseId,
        PublicUserVO reporter,
        ReportTargetType targetType,
        Long targetId,
        String reasonCategory,
        String description,
        List<String> evidenceUrls,
        ReportStatus status,
        ReportDecisionType decisionType,
        Instant createdAt
) {
    public static ReportCaseVO from(ReportCase c, PublicUserVO reporter) {
        List<String> urls = (c.getEvidenceUrls() == null || c.getEvidenceUrls().isBlank())
                ? List.of()
                : Arrays.stream(c.getEvidenceUrls().split(",")).filter(s -> !s.isBlank()).toList();
        return new ReportCaseVO(
                c.getId(),
                reporter,
                c.getTargetType(),
                c.getTargetId(),
                c.getReasonCategory(),
                c.getDescription(),
                urls,
                c.getStatus(),
                c.getDecisionType(),
                c.getCreatedAt()
        );
    }
}
