package com.campushub.admin.controller;

import com.campushub.admin.dto.AdminAppealResolveDTO;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.credit.service.AppealService;
import com.campushub.credit.vo.CreditAppealVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/admin/appeals/* —— 信用差评申诉裁决（闭合 F-CREDIT-05~07）。 */
@RestController
@RequestMapping("/api/admin/appeals")
public class AdminAppealController {

    private final AppealService appealService;

    public AdminAppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    /** 待处理申诉队列。 */
    @GetMapping
    public ApiResponse<List<CreditAppealVO>> pending() {
        return ApiResponse.success(appealService.listPending());
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> resolve(@PathVariable("id") long appealId,
                                      @Valid @RequestBody AdminAppealResolveDTO dto) {
        appealService.resolve(CurrentUserHolder.getUserId(), appealId, dto.getApprove(), dto.getNote());
        return ApiResponse.success();
    }
}
