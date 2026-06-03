package com.campushub.report.service;

import com.campushub.admin.dto.AdminReportDecisionDTO;
import com.campushub.report.dto.ReportCreateDTO;
import com.campushub.report.vo.ReportCaseVO;

import java.util.List;

/** 举报 / 仲裁（F-REPORT-01/03/04，简化版）。 */
public interface ReportService {

    /** 用户提交举报。 */
    ReportCaseVO submit(long reporterId, ReportCreateDTO dto);

    /** 我的举报。 */
    List<ReportCaseVO> listMy(long reporterId);

    /** 待处理案件队列（管理员）。 */
    List<ReportCaseVO> listPending();

    /** 管理员裁决并执行（驳回 / 警告 / 扣分）。 */
    void decide(long adminId, long caseId, AdminReportDecisionDTO dto);
}
