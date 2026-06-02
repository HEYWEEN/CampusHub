package com.campushub.team.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.team.entity.TeamApplication;
import com.campushub.team.entity.TeamApplicationStatus;

import java.time.Instant;

public record TeamApplicationVO(
        Long applicationId,
        PublicUserVO applicant,
        int creditScore,
        String message,
        TeamApplicationStatus status,
        Instant createdAt
) {
    public static TeamApplicationVO from(TeamApplication a, PublicUserVO applicant, int creditScore) {
        return new TeamApplicationVO(
                a.getId(),
                applicant,
                creditScore,
                a.getMessage(),
                a.getStatus(),
                a.getCreatedAt()
        );
    }
}
