package com.campushub.team.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.exception.BizException;
import com.campushub.credit.api.CreditApi;
import com.campushub.notify.api.NotifyApi;
import com.campushub.team.dto.TeamApplicationCreateDTO;
import com.campushub.team.dto.TeamApplicationReviewDTO;
import com.campushub.team.dto.TeamRecruitCreateDTO;
import com.campushub.team.entity.TeamApplication;
import com.campushub.team.entity.TeamApplicationStatus;
import com.campushub.team.entity.TeamRecruit;
import com.campushub.team.entity.TeamRecruitStatus;
import com.campushub.team.repository.TeamApplicationRepository;
import com.campushub.team.repository.TeamRecruitRepository;
import com.campushub.team.vo.TeamRecruitVO;
import com.campushub.user.api.UserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
