package com.campushub.admin.controller;

import com.campushub.admin.dto.AdminReportDecisionDTO;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.report.service.ReportService;
import com.campushub.report.vo.ReportCaseVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/admin/reports/* —— 仲裁案件队列 + 裁决（F-REPORT-02 简化 / 03 / 04）。 */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 待处理案件队列。 */
    @GetMapping
    public ApiResponse<List<ReportCaseVO>> pending() {
        return ApiResponse.success(reportService.listPending());
    }

    /** 裁决执行（驳回 / 警告 / 扣分）。 */
    @PostMapping("/{id}/decision")
    public ApiResponse<Void> decide(@PathVariable("id") long caseId,
                                    @Valid @RequestBody AdminReportDecisionDTO dto) {
        reportService.decide(CurrentUserHolder.getUserId(), caseId, dto);
        return ApiResponse.success();
    }
}
