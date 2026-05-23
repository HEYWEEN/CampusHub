package com.campushub.edu.vo;

import java.time.Instant;

public record TutorTaskVO(
        Long id,
        Long publisherId,
        String subject,
        String description,
        int rewardPoint,
        int status,
        Instant createdAt
) {}
