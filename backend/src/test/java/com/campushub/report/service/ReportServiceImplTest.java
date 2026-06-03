package com.campushub.report.service;

import com.campushub.admin.dto.AdminReportDecisionDTO;
import com.campushub.common.PublicUserVO;
import com.campushub.common.exception.BizException;
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
import com.campushub.task.api.TaskApi;
import com.campushub.user.api.UserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReportServiceImpl 单测（Mockito）：提交（目标解析/自举报/去重）+ 裁决（驳回/警告/扣分/边界）。
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock ReportCaseRepository caseRepo;
    @Mock ReportDecisionRepository decisionRepo;
    @Mock UserApi userApi;
    @Mock TaskApi taskApi;
    @Mock CreditApi creditApi;
    @Mock NotifyApi notifyApi;
    @Mock ApplicationEventPublisher events;

    @InjectMocks ReportServiceImpl service;

    private static final long REPORTER = 1L;

    /** save 时模拟 DB 回填自增 id（真实运行 save 后 id 非空）。 */
    private void caseSaveAssignsId(long id) {
        when(caseRepo.save(any(ReportCase.class))).thenAnswer(i -> {
            ReportCase c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", id);
            return c;
        });
    }

    private ReportCreateDTO dto(ReportTargetType type, long targetId) {
        ReportCreateDTO d = new ReportCreateDTO();
        d.setTargetType(type);
        d.setTargetId(targetId);
        d.setReasonCategory("FRAUD");
        d.setDescription("  涉嫌诈骗  ");
        return d;
    }

    private AdminReportDecisionDTO decision(ReportDecisionType type, Integer penalty) {
        AdminReportDecisionDTO d = new AdminReportDecisionDTO();
        d.setDecisionType(type);
        d.setPenaltyPoints(penalty);
        d.setReason("核实违规");
        return d;
    }

    // ==================== submit ====================

    @Test
    void submit_userTarget_savesAndPublishesEvent() {
        when(userApi.exists(99L)).thenReturn(true);
        when(caseRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                anyLong(), any(), anyLong(), eq(ReportStatus.PENDING))).thenReturn(false);
        caseSaveAssignsId(123L);

        service.submit(REPORTER, dto(ReportTargetType.USER, 99L));

        verify(caseRepo).save(argThat(c ->
                c.getReportedUserId() == 99L
                        && c.getReasonCategory().equals("FRAUD")
                        && c.getDescription().equals("涉嫌诈骗"))); // trim 生效
        verify(events).publishEvent(any(ReportSubmittedEvent.class));
    }

    @Test
    void submit_taskTarget_resolvesPublisherAsReported() {
        when(taskApi.getPublisherId(7L)).thenReturn(42L);
        when(caseRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                anyLong(), any(), anyLong(), any())).thenReturn(false);
        caseSaveAssignsId(123L);

        service.submit(REPORTER, dto(ReportTargetType.TASK, 7L));

        verify(caseRepo).save(argThat(c -> c.getReportedUserId() == 42L));
    }

    @Test
    void submit_tradeTarget_reportedUserNull() {
        when(caseRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                anyLong(), any(), anyLong(), any())).thenReturn(false);
        caseSaveAssignsId(123L);

        service.submit(REPORTER, dto(ReportTargetType.TRADE, 3L));

        verify(caseRepo).save(argThat(c -> c.getReportedUserId() == null));
        verifyNoInteractions(taskApi);
    }

    @Test
    void submit_selfReport_throws() {
        when(userApi.exists(REPORTER)).thenReturn(true);
        BizException ex = assertThrows(BizException.class,
                () -> service.submit(REPORTER, dto(ReportTargetType.USER, REPORTER)));
        assertEquals(ReportErrorCode.SELF_REPORT, ex.getCode());
        verify(caseRepo, never()).save(any());
    }

    @Test
    void submit_duplicatePending_throws() {
        when(userApi.exists(99L)).thenReturn(true);
        when(caseRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                REPORTER, ReportTargetType.USER, 99L, ReportStatus.PENDING)).thenReturn(true);
        BizException ex = assertThrows(BizException.class,
                () -> service.submit(REPORTER, dto(ReportTargetType.USER, 99L)));
        assertEquals(ReportErrorCode.DUPLICATE_PENDING, ex.getCode());
    }

    // ==================== decide ====================

    private ReportCase pendingCase(Long reportedUserId) {
        return new ReportCase(REPORTER, ReportTargetType.USER, 99L, reportedUserId,
                "FRAUD", "x", null);
    }

    @Test
    void decide_caseNotPending_throws() {
        ReportCase c = pendingCase(99L);
        c.resolve(ReportStatus.RESOLVED, 3L, ReportDecisionType.WARN); // 已处理
        when(caseRepo.findById(1L)).thenReturn(Optional.of(c));

        BizException ex = assertThrows(BizException.class,
                () -> service.decide(3L, 1L, decision(ReportDecisionType.WARN, null)));
        assertEquals(ReportErrorCode.CASE_NOT_PENDING, ex.getCode());
    }

    @Test
    void decide_dismiss_setsDismissedAndNotifiesReporter() {
        ReportCase c = pendingCase(99L);
        when(caseRepo.findById(1L)).thenReturn(Optional.of(c));

        service.decide(3L, 1L, decision(ReportDecisionType.DISMISS, null));

        assertEquals(ReportStatus.DISMISSED, c.getStatus());
        verify(notifyApi).appendLetter(eq(REPORTER), eq("REPORT_RESULT"), anyString(), anyString(), anyString());
        verify(creditApi, never()).deduct(anyLong(), anyInt(), anyString(), anyString());
        verify(decisionRepo).save(any(ReportDecision.class));
    }

    @Test
    void decide_warn_notifiesReporterAndReported() {
        ReportCase c = pendingCase(99L);
        when(caseRepo.findById(1L)).thenReturn(Optional.of(c));

        service.decide(3L, 1L, decision(ReportDecisionType.WARN, null));

        assertEquals(ReportStatus.RESOLVED, c.getStatus());
        verify(notifyApi).appendLetter(eq(REPORTER), eq("REPORT_RESULT"), anyString(), anyString(), anyString());
        verify(notifyApi).appendLetter(eq(99L), eq("REPORT_ACTION"), anyString(), anyString(), anyString());
        verify(creditApi, never()).deduct(anyLong(), anyInt(), anyString(), anyString());
    }

    @Test
    void decide_penalize_deductsCreditAndNotifies() {
        ReportCase c = pendingCase(99L);
        when(caseRepo.findById(1L)).thenReturn(Optional.of(c));

        service.decide(3L, 1L, decision(ReportDecisionType.PENALIZE, 10));

        assertEquals(ReportStatus.RESOLVED, c.getStatus());
        verify(creditApi).deduct(eq(99L), eq(10), eq("REPORT_PENALTY"), contains("report:1:penalty"));
        verify(notifyApi).appendLetter(eq(99L), eq("REPORT_ACTION"), anyString(), contains("10"), anyString());
    }

    @Test
    void decide_penalize_unknownReportedUser_throws() {
        ReportCase c = pendingCase(null); // TRADE 类目标解析不到被举报人
        when(caseRepo.findById(1L)).thenReturn(Optional.of(c));

        BizException ex = assertThrows(BizException.class,
                () -> service.decide(3L, 1L, decision(ReportDecisionType.PENALIZE, 10)));
        assertEquals(ReportErrorCode.PENALIZE_TARGET_UNKNOWN, ex.getCode());
        verify(creditApi, never()).deduct(anyLong(), anyInt(), anyString(), anyString());
    }

    @Test
    void decide_penalize_nonPositivePoints_throws() {
        ReportCase c = pendingCase(99L);
        when(caseRepo.findById(1L)).thenReturn(Optional.of(c));

        BizException ex = assertThrows(BizException.class,
                () -> service.decide(3L, 1L, decision(ReportDecisionType.PENALIZE, 0)));
        assertEquals(ReportErrorCode.INVALID_PENALTY, ex.getCode());
    }
}
