package com.campushub.credit.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.credit.dto.CreditAppealCreateDTO;
import com.campushub.credit.entity.AppealStatus;
import com.campushub.credit.entity.CreditAppeal;
import com.campushub.credit.entity.TaskReview;
import com.campushub.credit.exception.CreditErrorCode;
import com.campushub.credit.repository.CreditAppealRepository;
import com.campushub.credit.repository.TaskReviewRepository;
import com.campushub.credit.vo.CreditAppealVO;
import com.campushub.credit.vo.ReceivedReviewVO;
import com.campushub.notify.api.NotifyApi;
import com.campushub.user.api.UserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AppealServiceImpl implements AppealService {

    private static final int NEGATIVE_MAX_RATING = 2;   // ≤2 算差评
    private static final int WINDOW_DAYS = 7;
    private static final int MAX_PER_WINDOW = 3;

    private final CreditAppealRepository appealRepo;
    private final TaskReviewRepository reviewRepo;
    private final UserApi userApi;
    private final NotifyApi notifyApi;

    public AppealServiceImpl(CreditAppealRepository appealRepo, TaskReviewRepository reviewRepo,
                             UserApi userApi, NotifyApi notifyApi) {
        this.appealRepo = appealRepo;
        this.reviewRepo = reviewRepo;
        this.userApi = userApi;
        this.notifyApi = notifyApi;
    }

    @Override
    @Transactional
    public CreditAppealVO submitAppeal(long userId, CreditAppealCreateDTO dto) {
        TaskReview review = reviewRepo.findById(dto.getReviewId())
                .orElseThrow(() -> new NotFoundException("评价不存在: " + dto.getReviewId()));

        if (review.getRevieweeId() != userId) {
            throw new BizException(CreditErrorCode.APPEAL_NOT_REVIEWEE, "只能申诉自己收到的评价", 403);
        }
        if (review.getRating() > NEGATIVE_MAX_RATING) {
            throw new BizException(CreditErrorCode.APPEAL_NOT_NEGATIVE, "只有差评（≤2 分）可申诉");
        }
        Instant windowStart = Instant.now().minus(Duration.ofDays(WINDOW_DAYS));
        if (review.getCreatedAt().isBefore(windowStart)) {
            throw new BizException(CreditErrorCode.APPEAL_WINDOW_CLOSED, "差评已超过 7 天申诉窗口", 422);
        }
        if (appealRepo.countByAppellantIdAndCreatedAtAfter(userId, windowStart) >= MAX_PER_WINDOW) {
            throw new BizException(CreditErrorCode.APPEAL_RATE_LIMIT, "7 日内申诉次数已达上限(3)", 422);
        }

        String evidence = dto.getEvidenceUrls() == null ? null
                : dto.getEvidenceUrls().stream().filter(s -> s != null && !s.isBlank())
                    .limit(5).reduce((a, b) -> a + "," + b).orElse(null);

        CreditAppeal appeal = appealRepo.save(
                new CreditAppeal(review.getId(), userId, dto.getReason().trim(), evidence));
        return CreditAppealVO.from(appeal, review, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAppealVO> listMyAppeals(long userId) {
        return appealRepo.findByAppellantIdAndHiddenFalseOrderByCreatedAtDesc(userId).stream()
                .map(a -> CreditAppealVO.from(a, reviewRepo.findById(a.getReviewId()).orElse(null), null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedReviewVO> listReceivedReviews(long userId) {
        Instant windowStart = Instant.now().minus(Duration.ofDays(WINDOW_DAYS));
        return reviewRepo.findByRevieweeIdAndHiddenByRevieweeFalseOrderByCreatedAtDesc(userId).stream()
                .map(r -> {
                    boolean underAppeal = appealRepo.existsByReviewIdAndStatus(r.getId(), AppealStatus.PENDING);
                    boolean appealable = !r.isVoided()
                            && r.getRating() <= NEGATIVE_MAX_RATING
                            && !r.getCreatedAt().isBefore(windowStart)
                            && !underAppeal;
                    return ReceivedReviewVO.from(r, userApi.getPublicUser(r.getReviewerId()), underAppeal, appealable);
                })
                .toList();
    }

    @Override
    @Transactional
    public void hideReceivedReview(long userId, long reviewId) {
        TaskReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("评价不存在: " + reviewId));
        if (review.getRevieweeId() != userId) {
            throw new NotFoundException("评价不存在: " + reviewId);   // 非本人：当作不存在，不泄露
        }
        if (!review.isVoided()) {
            throw new BizException(CreditErrorCode.REVIEW_NOT_HIDABLE, "仅已撤销的评价可删除", 422);
        }
        review.hideByReviewee();
        reviewRepo.save(review);
    }

    @Override
    @Transactional
    public void hideAppeal(long userId, long appealId) {
        CreditAppeal appeal = appealRepo.findById(appealId)
                .orElseThrow(() -> new NotFoundException("申诉不存在: " + appealId));
        if (appeal.getAppellantId() != userId) {
            throw new NotFoundException("申诉不存在: " + appealId);    // 非本人：当作不存在
        }
        if (appeal.getStatus() == AppealStatus.PENDING) {
            throw new BizException(CreditErrorCode.APPEAL_NOT_HIDABLE, "待处理的申诉不可删除", 409);
        }
        appeal.hide();
        appealRepo.save(appeal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAppealVO> listPending() {
        return appealRepo.findByStatusOrderByCreatedAtAsc(AppealStatus.PENDING).stream()
                .map(a -> CreditAppealVO.from(a,
                        reviewRepo.findById(a.getReviewId()).orElse(null),
                        userApi.getPublicUser(a.getAppellantId())))
                .toList();
    }

    @Override
    @Transactional
    public void resolve(long adminId, long appealId, boolean approve, String note) {
        CreditAppeal appeal = appealRepo.findById(appealId)
                .orElseThrow(() -> new NotFoundException("申诉不存在: " + appealId));
        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BizException(CreditErrorCode.APPEAL_NOT_PENDING, "该申诉已处理", 409);
        }

        appeal.resolve(approve ? AppealStatus.APPROVED : AppealStatus.REJECTED, adminId, note);
        if (approve) {
            reviewRepo.findById(appeal.getReviewId()).ifPresent(r -> {
                r.markVoided();            // 撤销差评（公开渲染打码）
                reviewRepo.save(r);
            });
        }
        appealRepo.save(appeal);

        String title = approve ? "申诉通过" : "申诉被驳回";
        String body = approve ? "你的差评申诉已通过，该评价已撤销 ✅"
                : ("申诉未通过" + (note == null || note.isBlank() ? "" : "：" + note));
        notifyApi.appendLetter(appeal.getAppellantId(), "CREDIT_APPEAL_RESULT", title, body,
                "CREDIT_APPEAL:" + appeal.getId() + ":" + appeal.getAppellantId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUnderAppeal(long reviewId) {
        return appealRepo.existsByReviewIdAndStatus(reviewId, AppealStatus.PENDING);
    }
}
