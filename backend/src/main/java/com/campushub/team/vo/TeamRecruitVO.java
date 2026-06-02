package com.campushub.team.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.team.entity.TeamApplication;
import com.campushub.team.entity.TeamApplicationStatus;
import com.campushub.team.entity.TeamRecruit;
import com.campushub.team.entity.TeamRecruitStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record TeamRecruitVO(
        Long recruitId,
        String title,
        String description,
        List<String> skillTags,
        int totalSize,
        int currentSize,
        TeamRecruitStatus status,
        PublicUserVO creator,
        Instant createdAt,
        boolean isCreator,
        TeamApplicationStatus myApplicationStatus
) {
    public static List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static TeamRecruitVO from(TeamRecruit r, PublicUserVO creator,
                                     boolean isCreator, TeamApplication myApp) {
        return new TeamRecruitVO(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                splitTags(r.getSkillTags()),
                r.getTotalSize(),
                r.getCurrentSize(),
                r.getStatus(),
                creator,
                r.getCreatedAt(),
                isCreator,
                myApp == null ? null : myApp.getStatus()
        );
    }
}
