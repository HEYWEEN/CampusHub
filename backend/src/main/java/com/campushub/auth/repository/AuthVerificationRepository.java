package com.campushub.auth.repository;

import com.campushub.auth.entity.AuthVerification;
import com.campushub.auth.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthVerificationRepository extends JpaRepository<AuthVerification, Long> {

    /** 取该用户最新一次提交（任意状态）— 用于 /verifications/me */
    Optional<AuthVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /** 是否存在 PENDING 中的申请（用于拒绝重复提交） */
    boolean existsByUserIdAndStatus(Long userId, VerificationStatus status);

    /** admin 待审核队列（最早提交优先） */
    List<AuthVerification> findByStatusOrderByCreatedAtAsc(VerificationStatus status);

    /** 是否已有「他人」用同一学号哈希处于指定状态（跨账号学号查重，bug 14） */
    boolean existsByStudentNoHashAndStatusAndUserIdNot(String studentNoHash, VerificationStatus status, Long userId);
}
