package com.campushub.team.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.exception.BizException;
import com.campushub.common.response.PageResponse;
import com.campushub.credit.api.CreditApi;
import com.campushub.notify.api.NotifyApi;
import com.campushub.team.dto.TeamApplicationCreateDTO;
import com.campushub.team.dto.TeamApplicationReviewDTO;
import com.campushub.team.dto.TeamRecruitCreateDTO;
import com.campushub.team.dto.TeamRecruitQueryDTO;
import com.campushub.team.entity.TeamApplication;
import com.campushub.team.entity.TeamApplicationStatus;
import com.campushub.team.entity.TeamRecruit;
import com.campushub.team.entity.TeamRecruitStatus;
import com.campushub.team.exception.TeamErrorCode;
import com.campushub.team.repository.TeamApplicationRepository;
import com.campushub.team.repository.TeamRecruitRepository;
import com.campushub.team.vo.TeamApplicationVO;
import com.campushub.team.vo.TeamRecruitVO;
import com.campushub.user.api.UserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock TeamRecruitRepository recruitRepo;
    @Mock TeamApplicationRepository appRepo;
    @Mock UserApi userApi;
    @Mock CreditApi creditApi;
    @Mock NotifyApi notifyApi;

    @InjectMocks TeamServiceImpl service;

    private TeamRecruitCreateDTO createDto() {
        TeamRecruitCreateDTO dto = new TeamRecruitCreateDTO();
        dto.setTitle("数模国赛三缺一");
        dto.setDescription("缺一个会编程的");
        dto.setSkillTags(List.of("数学建模", "Python"));
        dto.setTotalSize(3);
        return dto;
    }

    @Test
    void createRecruit_joinsTagsAndReturnsVO() {
        when(recruitRepo.save(any(TeamRecruit.class))).thenAnswer(i -> i.getArgument(0));
        when(userApi.getPublicUser(100L)).thenReturn(new PublicUserVO(100L, "队长", null, null));

        TeamRecruitVO vo = service.createRecruit(100L, createDto());

        assertEquals("数模国赛三缺一", vo.title());
        assertEquals(List.of("数学建模", "Python"), vo.skillTags());
        assertEquals(1, vo.currentSize());
        assertEquals(TeamRecruitStatus.RECRUITING, vo.status());
        assertTrue(vo.isCreator());
    }

    @Test
    void apply_selfApply_throws() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 3);
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        BizException ex = assertThrows(BizException.class,
                () -> service.apply(100L, 5L, new TeamApplicationCreateDTO()));
        assertEquals(7003, ex.getCode());
        verify(appRepo, never()).save(any());
    }

    @Test
    void review_approve_incrementsSizeAndNotifies() {
        TeamRecruit r = new TeamRecruit(100L, "数模", "d", "java", 2); // total=2, current=1
        TeamApplication app = new TeamApplication(5L, 200L, "带我");
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        TeamApplicationReviewDTO dto = new TeamApplicationReviewDTO();
        dto.setApprove(true);
        service.review(100L, 1L, dto);

        assertEquals(TeamApplicationStatus.APPROVED, app.getStatus());
        assertEquals(2, r.getCurrentSize());
        assertEquals(TeamRecruitStatus.FULL, r.getStatus());   // 满员自动 FULL
        verify(notifyApi).appendLetter(eq(200L), eq("TEAM_APPLY_RESULT"), anyString(), anyString(), anyString());
    }

    @Test
    void review_notCaptain_throws() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 3);
        TeamApplication app = new TeamApplication(5L, 200L, "带我");
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        TeamApplicationReviewDTO dto = new TeamApplicationReviewDTO();
        dto.setApprove(true);

        BizException ex = assertThrows(BizException.class, () -> service.review(999L, 1L, dto));
        assertEquals(7001, ex.getCode());
        verify(notifyApi, never()).appendLetter(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    // ==================== F-TEAM-04 大厅查询 / 详情 ====================

    @Test
    @SuppressWarnings("unchecked")
    void search_mapsRecruitsToVO() {
        TeamRecruit r = new TeamRecruit(100L, "数模", "缺人", "Python", 3);
        Page<TeamRecruit> page = new PageImpl<>(List.of(r));
        when(recruitRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userApi.getPublicUser(100L)).thenReturn(new PublicUserVO(100L, "队长", null, null));

        TeamRecruitQueryDTO q = new TeamRecruitQueryDTO();
        q.setStatus(TeamRecruitStatus.RECRUITING);
        q.setTag("Python");
        q.setQ("数模");

        PageResponse<TeamRecruitVO> res = service.search(q, 100L);
        assertEquals(1, res.getItems().size());
        assertTrue(res.getItems().get(0).isCreator()); // currentUserId == creatorId
    }

    @Test
    void getDetail_includesMyLatestApplication() {
        TeamRecruit r = new TeamRecruit(100L, "数模", "d", "java", 3);
        TeamApplication myApp = new TeamApplication(5L, 200L, "带我");
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));
        when(appRepo.findFirstByRecruitIdAndApplicantIdOrderByCreatedAtDesc(5L, 200L))
                .thenReturn(Optional.of(myApp));
        when(userApi.getPublicUser(100L)).thenReturn(new PublicUserVO(100L, "队长", null, null));

        TeamRecruitVO vo = service.getDetail(5L, 200L);
        assertEquals("数模", vo.title());
        assertFalse(vo.isCreator()); // 访客视角
    }

    // ==================== F-TEAM-03 队长查看申请 ====================

    @Test
    void listApplications_notCaptain_throws() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 3);
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        BizException ex = assertThrows(BizException.class, () -> service.listApplications(999L, 5L));
        assertEquals(TeamErrorCode.NOT_CAPTAIN, ex.getCode());
    }

    @Test
    void listApplications_captain_mapsVOWithCreditScore() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 3);
        TeamApplication app = new TeamApplication(5L, 200L, "带我");
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));
        when(appRepo.findByRecruitIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(app));
        when(userApi.getPublicUser(200L)).thenReturn(new PublicUserVO(200L, "申请人", null, null));
        when(creditApi.getScoreOf(200L)).thenReturn(85);

        List<TeamApplicationVO> vos = service.listApplications(100L, 5L);
        assertEquals(1, vos.size());
        assertEquals(85, vos.get(0).creditScore());
        assertEquals("带我", vos.get(0).message());
        assertEquals(TeamApplicationStatus.PENDING, vos.get(0).status());
    }

    // ==================== F-TEAM-02 申请分支 ====================

    @Test
    void apply_recruitClosed_throws() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 3);
        r.setStatus(TeamRecruitStatus.CLOSED);
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        BizException ex = assertThrows(BizException.class,
                () -> service.apply(200L, 5L, new TeamApplicationCreateDTO()));
        assertEquals(TeamErrorCode.RECRUIT_CLOSED, ex.getCode());
        verify(appRepo, never()).save(any());
    }

    @Test
    void apply_full_throws() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 1); // totalSize=1 → current=1 已满
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        BizException ex = assertThrows(BizException.class,
                () -> service.apply(200L, 5L, new TeamApplicationCreateDTO()));
        assertEquals(TeamErrorCode.ALREADY_FULL, ex.getCode());
    }

    @Test
    void apply_rateLimit_throws() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 3);
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));
        when(appRepo.countByRecruitIdAndApplicantIdAndCreatedAtAfter(eq(5L), eq(200L), any(Instant.class)))
                .thenReturn(3L);

        BizException ex = assertThrows(BizException.class,
                () -> service.apply(200L, 5L, new TeamApplicationCreateDTO()));
        assertEquals(TeamErrorCode.APPLY_RATE_LIMIT, ex.getCode());
        verify(appRepo, never()).save(any());
    }

    @Test
    void apply_success_savesTrimmedMessage() {
        TeamRecruit r = new TeamRecruit(100L, "t", "d", "java", 3);
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));
        when(appRepo.countByRecruitIdAndApplicantIdAndCreatedAtAfter(anyLong(), anyLong(), any(Instant.class)))
                .thenReturn(0L);

        TeamApplicationCreateDTO dto = new TeamApplicationCreateDTO();
        dto.setMessage("  我会 Java  ");
        service.apply(200L, 5L, dto);

        verify(appRepo).save(any(TeamApplication.class));
    }

    // ==================== F-TEAM-03 审核分支 ====================

    @Test
    void review_reject_notifiesAndSetsRejected() {
        TeamRecruit r = new TeamRecruit(100L, "数模", "d", "java", 3);
        TeamApplication app = new TeamApplication(5L, 200L, "带我");
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        TeamApplicationReviewDTO dto = new TeamApplicationReviewDTO();
        dto.setApprove(false);
        service.review(100L, 1L, dto);

        assertEquals(TeamApplicationStatus.REJECTED, app.getStatus());
        verify(recruitRepo, never()).save(any()); // 拒绝不动队伍人数
        verify(notifyApi).appendLetter(eq(200L), eq("TEAM_APPLY_RESULT"), anyString(), anyString(), anyString());
    }

    @Test
    void review_alreadyProcessed_throws() {
        TeamRecruit r = new TeamRecruit(100L, "数模", "d", "java", 3);
        TeamApplication app = new TeamApplication(5L, 200L, "带我");
        app.setStatus(TeamApplicationStatus.APPROVED); // 已处理
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        TeamApplicationReviewDTO dto = new TeamApplicationReviewDTO();
        dto.setApprove(true);
        BizException ex = assertThrows(BizException.class, () -> service.review(100L, 1L, dto));
        assertEquals(TeamErrorCode.APPLICATION_NOT_PENDING, ex.getCode());
    }

    @Test
    void review_approveButFull_throws() {
        TeamRecruit r = new TeamRecruit(100L, "数模", "d", "java", 1); // 已满
        TeamApplication app = new TeamApplication(5L, 200L, "带我");
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(recruitRepo.findById(5L)).thenReturn(Optional.of(r));

        TeamApplicationReviewDTO dto = new TeamApplicationReviewDTO();
        dto.setApprove(true);
        BizException ex = assertThrows(BizException.class, () -> service.review(100L, 1L, dto));
        assertEquals(TeamErrorCode.ALREADY_FULL, ex.getCode());
    }
}
