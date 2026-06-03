package com.campushub.admin.dto;

import com.campushub.report.entity.ReportDecisionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** POST /api/admin/reports/{id}/decision — 仲裁裁决（F-REPORT-03）。 */
public class AdminReportDecisionDTO {

    @NotNull
    private ReportDecisionType decisionType;

    /** 仅 PENALIZE 用；1~50 分。 */
    @Min(0)
    @Max(50)
    private Integer penaltyPoints;

    @Size(max = 500)
    private String reason;

    public ReportDecisionType getDecisionType() { return decisionType; }
    public void setDecisionType(ReportDecisionType decisionType) { this.decisionType = decisionType; }
    public Integer getPenaltyPoints() { return penaltyPoints; }
    public void setPenaltyPoints(Integer penaltyPoints) { this.penaltyPoints = penaltyPoints; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
