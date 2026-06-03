package com.campushub.task.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.credit.api.CreditApi;
import com.campushub.task.dto.TaskCreateDTO;
import com.campushub.task.dto.TaskQueryDTO;
import com.campushub.task.dto.TaskUpdateDTO;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskAttachment;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;
import com.campushub.task.event.TaskAcceptedEvent;
import com.campushub.task.event.TaskCanceledEvent;
import com.campushub.task.event.TaskCompletedEvent;
import com.campushub.task.exception.TaskErrorCode;
import com.campushub.task.repository.TaskAttachmentRepository;
import com.campushub.task.repository.TaskExtendLogRepository;
import com.campushub.task.repository.TaskRepository;
import com.campushub.task.vo.AcceptLimitVO;
import com.campushub.task.vo.TaskDetailVO;
import com.campushub.task.vo.TaskExtendVO;
import com.campushub.task.vo.TaskProofVO;
import com.campushub.user.api.UserApi;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TaskServiceImpl 纯单测（Mockito）—— 覆盖 TASK-02~08 全部 service 分支：
 * 发布门槛 / 大厅查询 / 详情 / 编辑 / 取消(退押) / 接单(并发+限额) /
 * 凭证(校验) / 确认完成 / 延期(次数+范围) / 接单上限。
 *
 * <p>原本 task 模块仅有集成 happy-path 覆盖，service 异常分支为盲区
 * （见 jacoco：TaskServiceImpl 55% → 这里补齐）。
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepo;
    @Mock TaskAttachmentRepository attachRepo;
    @Mock TaskExtendLogRepository extendLogRepo;
    @Mock CreditApi creditApi;
    @Mock UserApi userApi;
    @Mock UserProfileRepository profileRepo;
    @Mock ApplicationEventPublisher events;

    @InjectMocks TaskServiceImpl service;

    private static final long PUBLISHER = 100L;
    private static final long ACCEPTER = 200L;

    /** 构造一个指定状态的任务（reward=50 → 押金 10）。 */
    private Task task(TaskStatus status) {
        Task t = new Task(PUBLISHER, "帮取快递", TaskType.ERRAND, status,
                50, Instant.now().plus(2, ChronoUnit.HOURS),
                "菜鸢驿站", "紫金楼", "尽快");
        return t;
    }

    private void mockFind(long taskId, Task t) {
        when(taskRepo.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(t));
    }

    // ==================== TaskApi 跨模块接口 ====================

    @Test
    void taskApi_passthrough() {
        Task t = task(TaskStatus.IN_PROGRESS);
        mockFind(1L, t);
        when(taskRepo.countInProgressBy(ACCEPTER)).thenReturn(3);

        assertEquals(TaskStatus.IN_PROGRESS, service.getTaskStatus(1L));
        assertEquals(PUBLISHER, service.getPublisherId(1L));
        assertEquals(3, service.countInProgressBy(ACCEPTER));
    }

    @Test
    void findTask_missing_throwsNotFound() {
        when(taskRepo.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getTaskStatus(9L));
    }

    // ==================== create 发布门槛 ====================

    private TaskCreateDTO createDto() {
        TaskCreateDTO dto = new TaskCreateDTO();
        dto.setTitle("帮取快递");
        dto.setTaskType(TaskType.ERRAND);
        dto.setRewardPoint(50);
        dto.setDeadlineAt(Instant.now().plus(2, ChronoUnit.HOURS));
        dto.setDeliveryBuilding("紫金楼");
        return dto;
    }

    @Test
    void create_notVerified_throws() {
        when(userApi.getVerifyStatus(PUBLISHER)).thenReturn(VerifyStatus.GUEST);
        BizException ex = assertThrows(BizException.class, () -> service.create(PUBLISHER, createDto()));
        assertEquals(TaskErrorCode.NOT_VERIFIED, ex.getCode());
        verify(taskRepo, never()).save(any());
    }

    @Test
    void create_creditTooLow_throws() {
        when(userApi.getVerifyStatus(PUBLISHER)).thenReturn(VerifyStatus.APPROVED);
        when(creditApi.getScoreOf(PUBLISHER)).thenReturn(50);
        BizException ex = assertThrows(BizException.class, () -> service.create(PUBLISHER, createDto()));
        assertEquals(TaskErrorCode.CREDIT_TOO_LOW, ex.getCode());
        verify(creditApi, never()).freeze(anyLong(), anyInt(), anyString());
    }

    @Test
    void create_success_savesAndFreezesReward() {
        when(userApi.getVerifyStatus(PUBLISHER)).thenReturn(VerifyStatus.APPROVED);
        when(creditApi.getScoreOf(PUBLISHER)).thenReturn(80);
        when(taskRepo.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task t = service.create(PUBLISHER, createDto());

        assertEquals(TaskStatus.PENDING_ACCEPT, t.getStatus());
        verify(creditApi).freeze(eq(PUBLISHER), eq(50), contains(":freeze"));
    }

    // ==================== search 大厅查询 ====================

    @Test
    @SuppressWarnings("unchecked")
    void search_buildsSpecAndPageable() {
        Page<Task> page = new PageImpl<>(List.of(task(TaskStatus.PENDING_ACCEPT)));
        when(taskRepo.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        TaskQueryDTO q = new TaskQueryDTO();
        q.setTaskType(TaskType.ERRAND);
        q.setStatus(TaskStatus.PENDING_ACCEPT);
        q.setQ("快递");
        q.setSort("deadlineAt,asc");
        q.setPage(2);
        q.setSize(10);

        Page<Task> result = service.search(q);
        assertEquals(1, result.getContent().size());
        verify(taskRepo).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    // ==================== getDetail 详情 ====================

    @Test
    void getDetail_visitorCanAccept() {
        Task t = task(TaskStatus.PENDING_ACCEPT);
        mockFind(1L, t);
        when(userApi.getPublicUser(PUBLISHER)).thenReturn(new PublicUserVO(PUBLISHER, "发布者", null, null));
        when(attachRepo.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(creditApi.getScoreOf(ACCEPTER)).thenReturn(80);

        TaskDetailVO vo = service.getDetail(1L, ACCEPTER);
        assertTrue(vo.canAccept());
        assertFalse(vo.isPublisher());
    }

    @Test
    void getDetail_publisherViewing_cannotAccept() {
        Task t = task(TaskStatus.IN_PROGRESS);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);
        when(userApi.getPublicUser(PUBLISHER)).thenReturn(new PublicUserVO(PUBLISHER, "发布者", null, null));
        when(userApi.getPublicUser(ACCEPTER)).thenReturn(new PublicUserVO(ACCEPTER, "接单者", null, null));
        when(attachRepo.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        TaskDetailVO vo = service.getDetail(1L, PUBLISHER);
        assertTrue(vo.isPublisher());
        assertFalse(vo.canAccept());
        verify(creditApi, never()).getScoreOf(anyLong());
    }

    // ==================== update 编辑 ====================

    @Test
    void update_notPublisher_throws() {
        mockFind(1L, task(TaskStatus.PENDING_ACCEPT));
        BizException ex = assertThrows(BizException.class,
                () -> service.update(999L, 1L, new TaskUpdateDTO()));
        assertEquals(TaskErrorCode.NOT_PUBLISHER, ex.getCode());
    }

    @Test
    void update_success_editsFields() {
        Task t = task(TaskStatus.PENDING_ACCEPT);
        mockFind(1L, t);
        when(taskRepo.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskUpdateDTO dto = new TaskUpdateDTO();
        dto.setRemark("改了备注");
        dto.setDeliveryBuilding("仙林楼");
        service.update(PUBLISHER, 1L, dto);

        assertEquals("改了备注", t.getRemark());
        assertEquals("仙林楼", t.getDeliveryBuilding());
    }

    // ==================== cancel 取消 ====================

    @Test
    void cancel_notInvolved_throws() {
        mockFind(1L, task(TaskStatus.PENDING_ACCEPT));
        BizException ex = assertThrows(BizException.class, () -> service.cancel(999L, 1L, "不想要了"));
        assertEquals(TaskErrorCode.NOT_INVOLVED, ex.getCode());
    }

    @Test
    void cancel_pendingNoAssignee_unfreezesRewardOnly() {
        Task t = task(TaskStatus.PENDING_ACCEPT);
        mockFind(1L, t);

        service.cancel(PUBLISHER, 1L, "撤回");

        assertEquals(TaskStatus.CANCELED, t.getStatus());
        verify(creditApi).unfreeze(eq(PUBLISHER), eq(50), contains("unfreeze_reward"));
        verify(creditApi, never()).unfreeze(eq(ACCEPTER), anyInt(), anyString());
        verify(events).publishEvent(any(TaskCanceledEvent.class));
    }

    @Test
    void cancel_inProgress_refundsBothSides() {
        Task t = task(TaskStatus.IN_PROGRESS);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);

        service.cancel(PUBLISHER, 1L, "黄了");

        assertEquals(TaskStatus.CANCELED, t.getStatus());
        verify(creditApi).unfreeze(eq(PUBLISHER), eq(50), contains("unfreeze_reward"));
        verify(creditApi).unfreeze(eq(ACCEPTER), eq(10), contains("unfreeze_deposit"));
    }

    // ==================== accept 接单 ====================

    @Test
    void accept_creditTooLow_throws() {
        when(creditApi.getScoreOf(ACCEPTER)).thenReturn(40);
        BizException ex = assertThrows(BizException.class, () -> service.accept(ACCEPTER, 1L, 0));
        assertEquals(TaskErrorCode.CREDIT_TOO_LOW, ex.getCode());
        verify(taskRepo, never()).findByIdAndDeletedAtIsNull(anyLong());
    }

    @Test
    void accept_selfAccept_throws() {
        when(creditApi.getScoreOf(PUBLISHER)).thenReturn(80);
        mockFind(1L, task(TaskStatus.PENDING_ACCEPT));
        BizException ex = assertThrows(BizException.class, () -> service.accept(PUBLISHER, 1L, 0));
        assertEquals(TaskErrorCode.SELF_ACCEPT, ex.getCode());
    }

    @Test
    void accept_versionConflict_throws() {
        when(creditApi.getScoreOf(ACCEPTER)).thenReturn(80);
        mockFind(1L, task(TaskStatus.PENDING_ACCEPT)); // version=0
        BizException ex = assertThrows(BizException.class, () -> service.accept(ACCEPTER, 1L, 5));
        assertEquals(TaskErrorCode.VERSION_CONFLICT, ex.getCode());
    }

    @Test
    void accept_limitReached_throws() {
        when(creditApi.getScoreOf(ACCEPTER)).thenReturn(80);
        mockFind(1L, task(TaskStatus.PENDING_ACCEPT));
        when(taskRepo.countInProgressBy(ACCEPTER)).thenReturn(2);
        UserProfile p = mock(UserProfile.class);
        when(p.getDailyAcceptLimit()).thenReturn(2);
        when(profileRepo.findByUserId(ACCEPTER)).thenReturn(Optional.of(p));

        BizException ex = assertThrows(BizException.class, () -> service.accept(ACCEPTER, 1L, 0));
        assertEquals(TaskErrorCode.ACCEPT_LIMIT, ex.getCode());
        verify(creditApi, never()).freeze(anyLong(), anyInt(), anyString());
    }

    @Test
    void accept_success_freezesDepositAndPublishes() {
        when(creditApi.getScoreOf(ACCEPTER)).thenReturn(80);
        Task t = task(TaskStatus.PENDING_ACCEPT);
        mockFind(1L, t);
        when(taskRepo.countInProgressBy(ACCEPTER)).thenReturn(0);
        when(profileRepo.findByUserId(ACCEPTER)).thenReturn(Optional.empty()); // 默认上限 2

        service.accept(ACCEPTER, 1L, 0);

        assertEquals(TaskStatus.IN_PROGRESS, t.getStatus());
        assertEquals(ACCEPTER, t.getAssigneeId());
        verify(creditApi).freeze(eq(ACCEPTER), eq(10), contains(":deposit"));
        verify(events).publishEvent(any(TaskAcceptedEvent.class));
    }

    // ==================== submitProof 凭证 ====================

    @Test
    void submitProof_notAssignee_throws() {
        Task t = task(TaskStatus.IN_PROGRESS);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);
        BizException ex = assertThrows(BizException.class,
                () -> service.submitProof(999L, 1L, List.of(), "做完了"));
        assertEquals(TaskErrorCode.NOT_ASSIGNEE, ex.getCode());
    }

    @Test
    void submitProof_tooManyImages_throws() {
        Task t = task(TaskStatus.IN_PROGRESS);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);
        List<String> imgs = List.of("a", "b", "c", "d");
        BizException ex = assertThrows(BizException.class,
                () -> service.submitProof(ACCEPTER, 1L, imgs, null));
        assertEquals(TaskErrorCode.PROOF_IMAGE_TOO_MANY, ex.getCode());
    }

    @Test
    void submitProof_textTooLong_throws() {
        Task t = task(TaskStatus.IN_PROGRESS);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);
        String longText = "字".repeat(301);
        BizException ex = assertThrows(BizException.class,
                () -> service.submitProof(ACCEPTER, 1L, List.of(), longText));
        assertEquals(TaskErrorCode.PROOF_TEXT_TOO_LONG, ex.getCode());
    }

    @Test
    void submitProof_success_savesAttachmentsAndTransitions() {
        Task t = task(TaskStatus.IN_PROGRESS);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);

        TaskProofVO vo = service.submitProof(ACCEPTER, 1L,
                List.of("/uploads/1.jpg", "", "/uploads/2.jpg"), "已送达");

        assertEquals(TaskStatus.WAIT_CONFIRM, t.getStatus());
        assertEquals(2, vo.proofImages().size()); // 空串被过滤
        verify(attachRepo, times(2)).save(any(TaskAttachment.class));
    }

    // ==================== confirmComplete 确认完成 ====================

    @Test
    void confirmComplete_notPublisher_throws() {
        Task t = task(TaskStatus.WAIT_CONFIRM);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);
        BizException ex = assertThrows(BizException.class, () -> service.confirmComplete(999L, 1L));
        assertEquals(TaskErrorCode.NOT_PUBLISHER, ex.getCode());
    }

    @Test
    void confirmComplete_success_unfreezesDepositAndPublishes() {
        Task t = task(TaskStatus.WAIT_CONFIRM);
        t.setAssigneeId(ACCEPTER);
        mockFind(1L, t);

        service.confirmComplete(PUBLISHER, 1L);

        assertEquals(TaskStatus.COMPLETED, t.getStatus());
        verify(creditApi).unfreeze(eq(ACCEPTER), eq(10), contains("unfreeze_deposit"));
        verify(events).publishEvent(any(TaskCompletedEvent.class));
    }

    // ==================== extend 延期 ====================

    @Test
    void extend_notPublisher_throws() {
        mockFind(1L, task(TaskStatus.IN_PROGRESS));
        BizException ex = assertThrows(BizException.class, () -> service.extend(999L, 1L, 30));
        assertEquals(TaskErrorCode.NOT_PUBLISHER, ex.getCode());
    }

    @Test
    void extend_countLimit_throws() {
        mockFind(1L, task(TaskStatus.IN_PROGRESS));
        when(extendLogRepo.countByTaskId(1L)).thenReturn(2);
        BizException ex = assertThrows(BizException.class, () -> service.extend(PUBLISHER, 1L, 30));
        assertEquals(TaskErrorCode.EXTEND_LIMIT, ex.getCode());
    }

    @Test
    void extend_rangeInvalid_throws() {
        mockFind(1L, task(TaskStatus.IN_PROGRESS));
        when(extendLogRepo.countByTaskId(1L)).thenReturn(0);
        BizException ex = assertThrows(BizException.class, () -> service.extend(PUBLISHER, 1L, 999));
        assertEquals(TaskErrorCode.EXTEND_RANGE, ex.getCode());
    }

    @Test
    void extend_success_savesLogAndReturnsVO() {
        Task t = task(TaskStatus.IN_PROGRESS);
        Instant before = t.getDeadlineAt();
        mockFind(1L, t);
        when(extendLogRepo.countByTaskId(1L)).thenReturn(1);

        TaskExtendVO vo = service.extend(PUBLISHER, 1L, 30);

        assertEquals(2, vo.extendCount());
        assertEquals(before.plusSeconds(1800), vo.deadlineAt());
        assertEquals(before.plusSeconds(1800), t.getDeadlineAt());
        verify(extendLogRepo).save(any());
    }

    // ==================== updateAcceptLimit 接单上限 ====================

    @Test
    void updateAcceptLimit_profileMissing_throws() {
        when(profileRepo.findByUserId(ACCEPTER)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.updateAcceptLimit(ACCEPTER, 5));
    }

    @Test
    void updateAcceptLimit_success() {
        UserProfile p = mock(UserProfile.class);
        when(profileRepo.findByUserId(ACCEPTER)).thenReturn(Optional.of(p));

        AcceptLimitVO vo = service.updateAcceptLimit(ACCEPTER, 5);

        assertEquals(5, vo.dailyAcceptLimit());
        verify(p).setDailyAcceptLimit(5);
        verify(profileRepo).save(p);
    }
}
