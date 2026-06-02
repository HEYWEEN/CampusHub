package com.campushub.credit.service;

import com.campushub.common.exception.BizException;
import com.campushub.credit.dto.CreditAppealCreateDTO;
import com.campushub.credit.entity.AppealStatus;
import com.campushub.credit.entity.CreditAppeal;
import com.campushub.credit.entity.TaskReview;
import com.campushub.credit.repository.CreditAppealRepository;
import com.campushub.credit.repository.TaskReviewRepository;
import com.campushub.notify.api.NotifyApi;
import com.campushub.user.api.UserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
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
class AppealServiceTest {

    @Mock CreditAppealRepository appealRepo;
    @Mock TaskReviewRepository reviewRepo;
    @Mock UserApi userApi;
    @Mock NotifyApi notifyApi;

    @InjectMocks AppealServiceImpl service;

    private TaskReview review(long id, long revieweeId, int rating, Instant createdAt) {
        TaskReview r = new TaskReview(900L, 7L, revieweeId, rating, "态度差");
        ReflectionTestUtils.setField(r, "id", id);
        ReflectionTestUtils.setField(r, "createdAt", createdAt);
        return r;
    }

    private CreditAppealCreateDTO dto(long reviewId) {
        CreditAppealCreateDTO d = new CreditAppealCreateDTO();
        d.setReviewId(reviewId);
        d.setReason("我按时完成了，差评不实");
        return d;
    }

    @Test
    void submit_notReviewee_throws() {
        when(reviewRepo.findById(1L)).thenReturn(Optional.of(review(1L, 999L, 1, Instant.now())));
        BizException ex = assertThrows(BizException.class, () -> service.submitAppeal(100L, dto(1L)));
        assertEquals(10005, ex.getCode());
        verify(appealRepo, never()).save(any());
    }

    @Test
    void submit_notNegative_throws() {
        when(reviewRepo.findById(1L)).thenReturn(Optional.of(review(1L, 100L, 5, Instant.now())));
        BizException ex = assertThrows(BizException.class, () -> service.submitAppeal(100L, dto(1L)));
        assertEquals(10006, ex.getCode());
    }

    @Test
    void submit_windowClosed_throws() {
        when(reviewRepo.findById(1L))
                .thenReturn(Optional.of(review(1L, 100L, 1, Instant.now().minus(Duration.ofDays(8)))));
        BizException ex = assertThrows(BizException.class, () -> service.submitAppeal(100L, dto(1L)));
        assertEquals(10007, ex.getCode());
    }

    @Test
    void submit_happy_persists() {
        when(reviewRepo.findById(1L)).thenReturn(Optional.of(review(1L, 100L, 2, Instant.now())));
        when(appealRepo.countByAppellantIdAndCreatedAtAfter(eq(100L), any())).thenReturn(0L);
        when(appealRepo.save(any(CreditAppeal.class))).thenAnswer(i -> i.getArgument(0));

        var vo = service.submitAppeal(100L, dto(1L));
        assertEquals(AppealStatus.PENDING, vo.status());
        assertEquals(2, vo.reviewRating());
        verify(appealRepo).save(any(CreditAppeal.class));
    }

    @Test
    void resolve_approve_voidsReviewAndNotifies() {
        CreditAppeal appeal = new CreditAppeal(1L, 100L, "理由", null);
        ReflectionTestUtils.setField(appeal, "id", 50L);
        TaskReview r = review(1L, 100L, 1, Instant.now());
        when(appealRepo.findById(50L)).thenReturn(Optional.of(appeal));
        when(reviewRepo.findById(1L)).thenReturn(Optional.of(r));

        service.resolve(9L, 50L, true, "属实，撤销");

        assertEquals(AppealStatus.APPROVED, appeal.getStatus());
        assertTrue(r.isVoided());
        verify(reviewRepo).save(r);
        verify(notifyApi).appendLetter(eq(100L), eq("CREDIT_APPEAL_RESULT"), anyString(), anyString(), anyString());
    }

    @Test
    void resolve_notPending_throws() {
        CreditAppeal appeal = new CreditAppeal(1L, 100L, "理由", null);
        appeal.resolve(AppealStatus.REJECTED, 9L, "已处理");
        when(appealRepo.findById(50L)).thenReturn(Optional.of(appeal));

        BizException ex = assertThrows(BizException.class, () -> service.resolve(9L, 50L, true, "x"));
        assertEquals(10009, ex.getCode());
        verify(notifyApi, never()).appendLetter(anyLong(), anyString(), anyString(), anyString(), anyString());
    }
}
