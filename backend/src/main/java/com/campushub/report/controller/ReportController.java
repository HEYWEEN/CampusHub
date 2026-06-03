package com.campushub.report.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.report.dto.ReportCreateDTO;
import com.campushub.report.service.ReportService;
import com.campushub.report.vo.ReportCaseVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/reports/* —— 用户提交举报 + 我的举报（F-REPORT-01）。 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ApiResponse<ReportCaseVO> submit(@Valid @RequestBody ReportCreateDTO dto) {
        return ApiResponse.success(reportService.submit(CurrentUserHolder.getUserId(), dto));
    }

    @GetMapping("/me")
    public ApiResponse<List<ReportCaseVO>> myReports() {
        return ApiResponse.success(reportService.listMy(CurrentUserHolder.getUserId()));
    }
}
