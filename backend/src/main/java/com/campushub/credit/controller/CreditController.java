package com.campushub.credit.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.response.PageResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.credit.service.CreditServiceImpl;
import com.campushub.credit.vo.CreditMeVO;
import com.campushub.credit.vo.CreditRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信用/积分查询接口（F-CREDIT-01 / 08）。均需登录（非白名单）。
 * 路径用复数 /api/credits，与前端 api/credit.ts 一致；评价接口在 /api/credit/reviews。
 */
@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditServiceImpl creditService;

    public CreditController(CreditServiceImpl creditService) {
        this.creditService = creditService;
    }

    /** 我的信用分 + 积分总览。 */
    @GetMapping("/me")
    public ApiResponse<CreditMeVO> getMyCredit() {
        return ApiResponse.success(creditService.getMyCredit(CurrentUserHolder.getUserId()));
    }

    /** 我的积分流水（分页，page 从 1 开始，size 默认 20、最大 100）。 */
    @GetMapping("/me/records")
    public ApiResponse<PageResponse<CreditRecordVO>> getMyRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(creditService.getMyRecords(CurrentUserHolder.getUserId(), page, size));
    }
}
