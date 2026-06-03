package com.campushub.report.repository;

import com.campushub.report.entity.ReportCase;
import com.campushub.report.entity.ReportStatus;
import com.campushub.report.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCaseRepository extends JpaRepository<ReportCase, Long> {

    List<ReportCase> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<ReportCase> findByStatusOrderByCreatedAtAsc(ReportStatus status);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterId, ReportTargetType targetType, Long targetId, ReportStatus status);
}
