package com.campushub.credit.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.credit.dto.ReviewSubmitDTO;
import com.campushub.credit.service.ReviewService;
import com.campushub.credit.vo.ReviewResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信用评价接口（CRD-02）。需登录（非白名单）。
 */
@RestController
@RequestMapping("/api/credit")
public class CreditReviewController {

    private final ReviewService reviewService;

    public CreditReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 提交任务双向评分。reviewer 取自登录态。 */
    @PostMapping("/reviews")
    public ApiResponse<ReviewResultVO> submitReview(@Valid @RequestBody ReviewSubmitDTO dto) {
        long reviewerId = CurrentUserHolder.getUserId();
        return ApiResponse.success(reviewService.submit(reviewerId, dto));
    }
}
