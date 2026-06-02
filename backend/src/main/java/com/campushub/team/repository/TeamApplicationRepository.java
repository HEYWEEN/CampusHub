package com.campushub.team.repository;

import com.campushub.team.entity.TeamApplication;
import com.campushub.team.entity.TeamApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {

    /** 24h 限频：同一申请人对同一帖最近的申请数。 */
    long countByRecruitIdAndApplicantIdAndCreatedAtAfter(Long recruitId, Long applicantId, Instant after);

    List<TeamApplication> findByRecruitIdOrderByCreatedAtDesc(Long recruitId);

    /** 当前用户对某帖最新一条申请（渲染 myApplicationStatus）。 */
    Optional<TeamApplication> findFirstByRecruitIdAndApplicantIdOrderByCreatedAtDesc(Long recruitId, Long applicantId);

    boolean existsByRecruitIdAndApplicantIdAndStatus(Long recruitId, Long applicantId, TeamApplicationStatus status);
}
