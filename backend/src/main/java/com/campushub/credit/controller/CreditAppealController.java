package com.campushub.credit.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.credit.dto.CreditAppealCreateDTO;
import com.campushub.credit.service.AppealService;
import com.campushub.credit.vo.CreditAppealVO;
import com.campushub.credit.vo.ReceivedReviewVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
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
}
