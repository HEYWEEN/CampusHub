package com.campushub.auth.vo;

import com.campushub.auth.entity.VerificationStatus;

import java.time.Instant;
import java.util.List;

/**
 * 管理员审核学生证认证用 —— 含解密后的实名/学号（仅 admin 端返回，普通接口严禁）。
 */
public record AdminVerificationVO(
        Long verificationId,
        Long userId,
        String realName,
        String studentNo,
        String idCard,
        List<String> attachmentUrls,
        VerificationStatus status,
        Instant createdAt
) {}
