package com.campushub.credit.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.credit.dto.CreditAppealCreateDTO;
import com.campushub.credit.service.AppealService;
import com.campushub.credit.vo.CreditAppealVO;
import com.campushub.credit.vo.ReceivedReviewVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/credit/* —— 信用差评申诉（F-CREDIT-05~07）。 */
@RestController
@RequestMapping("/api/credit")
public class CreditAppealController {

    private final AppealService appealService;

    public CreditAppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    /** 我收到的评价（带可申诉标记）—— 申诉入口。 */
    @GetMapping("/reviews/received")
    public ApiResponse<List<ReceivedReviewVO>> receivedReviews() {
        return ApiResponse.success(appealService.listReceivedReviews(CurrentUserHolder.getUserId()));
    }

    /** 发起申诉。 */
    @PostMapping("/appeals")
    public ApiResponse<CreditAppealVO> submit(@Valid @RequestBody CreditAppealCreateDTO dto) {
        return ApiResponse.success(appealService.submitAppeal(CurrentUserHolder.getUserId(), dto));
    }

    /** 我的申诉列表。 */
    @GetMapping("/appeals/me")
    public ApiResponse<List<CreditAppealVO>> myAppeals() {
        return ApiResponse.success(appealService.listMyAppeals(CurrentUserHolder.getUserId()));
    }

    /** 从「我收到的评价」删除一条已撤销的差评（软删，仅本人视图）。 */
    @DeleteMapping("/reviews/received/{reviewId}")
    public ApiResponse<Void> hideReceivedReview(@PathVariable("reviewId") long reviewId) {
        appealService.hideReceivedReview(CurrentUserHolder.getUserId(), reviewId);
        return ApiResponse.success(null);
    }

    /** 从「我的申诉」删除一条已处理的申诉记录（软删，仅本人视图）。 */
    @DeleteMapping("/appeals/{appealId}")
    public ApiResponse<Void> hideAppeal(@PathVariable("appealId") long appealId) {
        appealService.hideAppeal(CurrentUserHolder.getUserId(), appealId);
        return ApiResponse.success(null);
    }
}
