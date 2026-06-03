package com.campushub.report.event;

import com.campushub.report.entity.ReportTargetType;

/** 举报提交事件（F-REPORT-01）。当前无强制监听器，留作扩展（如管理员提醒）。 */
public record ReportSubmittedEvent(
        long caseId,
        long reporterId,
        ReportTargetType targetType,
        long targetId
) {}
