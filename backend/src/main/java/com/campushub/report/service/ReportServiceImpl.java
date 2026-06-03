package com.campushub.report.service;

import com.campushub.admin.dto.AdminReportDecisionDTO;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.credit.api.CreditApi;
import com.campushub.notify.api.NotifyApi;
import com.campushub.report.dto.ReportCreateDTO;
import com.campushub.report.entity.ReportCase;
import com.campushub.report.entity.ReportDecision;
import com.campushub.report.entity.ReportDecisionType;
import com.campushub.report.entity.ReportStatus;
import com.campushub.report.entity.ReportTargetType;
import com.campushub.report.event.ReportSubmittedEvent;
import com.campushub.report.exception.ReportErrorCode;
import com.campushub.report.repository.ReportCaseRepository;
import com.campushub.report.repository.ReportDecisionRepository;
import com.campushub.report.vo.ReportCaseVO;
import com.campushub.task.api.TaskApi;
import com.campushub.user.api.UserApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 举报 / 仲裁实现（简化版）。
 *
 * <p>提交时解析被举报方（USER=本人 / TASK=发布者 / TRADE=无法解析）。裁决三选一：
 * 驳回 / 警告 / 扣信用分（走 {@link CreditApi#deduct}）。裁决写入 {@link ReportDecision} 存档。
 * 跨模块只走 {@link UserApi} / {@link TaskApi} / {@link CreditApi} / {@link NotifyApi}。
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final ReportCaseRepository caseRepo;
    private final ReportDecisionRepository decisionRepo;
    private final UserApi userApi;
    private final TaskApi taskApi;
    private final CreditApi creditApi;
    private final NotifyApi notifyApi;
    private final ApplicationEventPublisher events;

    public ReportServiceImpl(ReportCaseRepository caseRepo, ReportDecisionRepository decisionRepo,
                             UserApi userApi, TaskApi taskApi, CreditApi creditApi,
                             NotifyApi notifyApi, ApplicationEventPublisher events) {
        this.caseRepo = caseRepo;
        this.decisionRepo = decisionRepo;
        this.userApi = userApi;
        this.taskApi = taskApi;
        this.creditApi = creditApi;
        this.notifyApi = notifyApi;
        this.events = events;
    }

    @Override
    @Transactional
    public ReportCaseVO submit(long reporterId, ReportCreateDTO dto) {
        Long reportedUserId = resolveReportedUser(dto.getTargetType(), dto.getTargetId());

        if (reportedUserId != null && reportedUserId == reporterId) {
            throw new BizException(ReportErrorCode.SELF_REPORT, "不能举报自己", 400);
        }
        if (caseRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, dto.getTargetType(), dto.getTargetId(), ReportStatus.PENDING)) {
            throw new BizException(ReportErrorCode.DUPLICATE_PENDING, "你已举报过该对象，正在处理中", 409);
        }

        String evidence = dto.getEvidenceUrls() == null ? null
                : dto.getEvidenceUrls().stream().filter(s -> s != null && !s.isBlank())
                    .limit(5).reduce((a, b) -> a + "," + b).orElse(null);

        ReportCase saved = caseRepo.save(new ReportCase(
                reporterId, dto.getTargetType(), dto.getTargetId(), reportedUserId,
                dto.getReasonCategory().trim(),
                dto.getDescription() == null ? null : dto.getDescription().trim(),
                evidence));

        events.publishEvent(new ReportSubmittedEvent(
                saved.getId(), reporterId, dto.getTargetType(), dto.getTargetId()));

        return ReportCaseVO.from(saved, null);
    }

    /** 解析被举报方用户：USER=目标本人；TASK=发布者；TRADE=null（无 TradeApi）。 */
    private Long resolveReportedUser(ReportTargetType type, long targetId) {
        return switch (type) {
            case USER -> {
                if (!userApi.exists(targetId)) throw new NotFoundException("用户不存在: " + targetId);
                yield targetId;
            }
            case TASK -> taskApi.getPublisherId(targetId); // 任务不存在时内部抛 NotFound
            case TRADE -> null;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCaseVO> listMy(long reporterId) {
        return caseRepo.findByReporterIdOrderByCreatedAtDesc(reporterId).stream()
                .map(c -> ReportCaseVO.from(c, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCaseVO> listPending() {
        return caseRepo.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING).stream()
                .map(c -> ReportCaseVO.from(c, userApi.getPublicUser(c.getReporterId())))
                .toList();
    }

    @Override
    @Transactional
    public void decide(long adminId, long caseId, AdminReportDecisionDTO dto) {
        ReportCase c = caseRepo.findById(caseId)
                .orElseThrow(() -> new NotFoundException("举报案件不存在: " + caseId));
        if (c.getStatus() != ReportStatus.PENDING) {
            throw new BizException(ReportErrorCode.CASE_NOT_PENDING, "该案件已处理", 409);
        }

        ReportDecisionType type = dto.getDecisionType();
        int penalty = dto.getPenaltyPoints() == null ? 0 : dto.getPenaltyPoints();

        switch (type) {
            case DISMISS -> {
                c.resolve(ReportStatus.DISMISSED, adminId, type);
                notify(c.getReporterId(), "REPORT_RESULT", "举报未成立",
                        "你的举报经核实未予支持", reporterKey(c));
            }
            case WARN -> {
                c.resolve(ReportStatus.RESOLVED, adminId, type);
                notify(c.getReporterId(), "REPORT_RESULT", "举报已处理",
                        "你的举报已核实，已对被举报方作出警告", reporterKey(c));
                if (c.getReportedUserId() != null) {
                    notify(c.getReportedUserId(), "REPORT_ACTION", "收到平台警告",
                            "你被举报的内容经核实违规，请遵守平台规范", reportedKey(c));
                }
            }
            case PENALIZE -> {
                if (c.getReportedUserId() == null) {
                    throw new BizException(ReportErrorCode.PENALIZE_TARGET_UNKNOWN,
                            "被举报方无法确定，无法扣分（可改为警告/驳回）", 409);
                }
                if (penalty <= 0) {
                    throw new BizException(ReportErrorCode.INVALID_PENALTY, "扣分必须为正数", 400);
                }
                creditApi.deduct(c.getReportedUserId(), penalty, "REPORT_PENALTY",
                        "report:" + caseId + ":penalty");
                c.resolve(ReportStatus.RESOLVED, adminId, type);
                notify(c.getReporterId(), "REPORT_RESULT", "举报已处理",
                        "你的举报已核实，已对被举报方扣信用分", reporterKey(c));
                notify(c.getReportedUserId(), "REPORT_ACTION", "信用分被扣减",
                        "你被举报的内容经核实违规，已扣 " + penalty + " 信用分", reportedKey(c));
            }
        }

        caseRepo.save(c);
        decisionRepo.save(new ReportDecision(caseId, adminId, type, penalty, dto.getReason()));
    }

    private void notify(long userId, String type, String title, String body, String bizKey) {
        notifyApi.appendLetter(userId, type, title, body, bizKey);
    }

    private static String reporterKey(ReportCase c) {
        return "REPORT_RESULT:" + c.getId() + ":" + c.getReporterId();
    }

    private static String reportedKey(ReportCase c) {
        return "REPORT_ACTION:" + c.getId() + ":" + c.getReportedUserId();
    }
}
