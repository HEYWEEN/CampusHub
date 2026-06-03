package com.campushub.report.repository;

import com.campushub.report.entity.ReportDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 仲裁存档：仅追加 + 按 case 查询。 */
public interface ReportDecisionRepository extends JpaRepository<ReportDecision, Long> {

    Optional<ReportDecision> findByCaseId(Long caseId);
}
