package com.campushub.admin.controller;

import com.campushub.admin.dto.AdminRejectDTO;
import com.campushub.auth.service.VerificationService;
import com.campushub.auth.vo.AdminVerificationVO;
import com.campushub.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/admin/verifications/* —— 学生证认证审核（F-ADMIN-01）。 */
@RestController
@RequestMapping("/api/admin/verifications")
public class AdminVerificationController {

    private final VerificationService verificationService;

    public AdminVerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /** 待审核队列（含解密实名/学号）。 */
    @GetMapping
    public ApiResponse<List<AdminVerificationVO>> pending() {
        return ApiResponse.success(verificationService.adminListPending());
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable("id") long verificationId) {
        verificationService.adminApprove(verificationId);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable("id") long verificationId,
                                     @Valid @RequestBody AdminRejectDTO dto) {
        verificationService.adminReject(verificationId, dto.getReason().trim());
        return ApiResponse.success();
    }
}
