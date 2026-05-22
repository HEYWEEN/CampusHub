package com.campushub.credit.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import com.campushub.credit.api.CreditApi;
import com.campushub.credit.dto.ReviewSubmitDTO;
import com.campushub.credit.entity.TaskReview;
import com.campushub.credit.exception.CreditErrorCode;
import com.campushub.credit.repository.TaskReviewRepository;
import com.campushub.credit.strategy.ScoreRule;
import com.campushub.credit.vo.ReviewResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务双向评分服务（CRD-02）。
 *
 * <p>规则（P1 SRS FR-CRED-01）：
 * <ul>
 *   <li>一人对一任务只能评一次（唯一键，重复抛 409）</li>
 *   <li>不能给自己评价</li>
 *   <li>当双方都互评完成时，双方各 +1 信用分（{@link ScoreRule#TASK_COMPLETE_BONUS}）</li>
 * </ul>
 *
 * <p><b>待接入（依赖 B 的 TaskApi）</b>：当前未校验「任务确已完成」「评价人确为该任务参与方」，
 * 这两项需 B 暴露 TaskApi（如 getParticipants/状态查询）后补；先以唯一键 + 自评校验兜底。
 */
@Service
public class ReviewService {

    private final TaskReviewRepository reviewRepo;
    private final CreditApi creditApi;

    public ReviewService(TaskReviewRepository reviewRepo, CreditApi creditApi) {
        this.reviewRepo = reviewRepo;
        this.creditApi = creditApi;
    }

    @Transactional
    public ReviewResultVO submit(long reviewerId, ReviewSubmitDTO dto) {
        if (reviewerId == dto.getRevieweeId()) {
            throw new BizException(ResponseCode.BAD_REQUEST, "不能给自己评价");
        }
        if (reviewRepo.existsByTaskIdAndReviewerId(dto.getTaskId(), reviewerId)) {
            throw new BizException(CreditErrorCode.REVIEW_DUPLICATED, "该任务你已评价过", 409);
        }
        // TODO(B-TaskApi)：校验任务已完成 + reviewer/reviewee 确为该任务双方

        TaskReview saved = reviewRepo.save(new TaskReview(
                dto.getTaskId(), reviewerId, dto.getRevieweeId(), dto.getRating(), dto.getComment()));

        boolean bothReviewed = awardBonusIfBothReviewed(dto.getTaskId());
        return new ReviewResultVO(saved.getId(), bothReviewed);
    }

    /**
     * 若该任务两条评价都已到位，给双方各 +1（幂等：bizKey 含 reviewerId，重复评不会重复加）。
     * @return 是否已双方互评完成
     */
    private boolean awardBonusIfBothReviewed(long taskId) {
        List<TaskReview> reviews = reviewRepo.findByTaskId(taskId);
        if (reviews.size() < 2) {
            return false;
        }
        for (TaskReview r : reviews) {
            creditApi.deduct(r.getReviewerId(), 0, ScoreRule.TASK_COMPLETE_BONUS.reasonCode(),
                    "task:" + taskId + ":bonus:" + r.getReviewerId());
        }
        return true;
    }
}
