package com.campushub.auth.controller;

import com.campushub.auth.service.VerificationService;
import com.campushub.auth.vo.VerificationStatusVO;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.credit.api.CreditApi;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only 辅助端点（仅在非 prod profile 下注册到 Spring 容器）。
 *
 * 包含的接口：
 *   - POST /api/auth/verifications/me/dev-approve
 *     把当前用户最新一条 PENDING 认证直接置为 APPROVED，方便本地 / demo 跳过管理员审批环节。
 *     真实生产应通过 admin 模块走完整审批流（admin 模块当前空缺，是 P3+ 计划）。
 *
 * 整个类用 @Profile("!prod") 控制：spring.profiles.active=prod 时这个 bean 不存在，
 * 接口也就不存在，生产不会泄漏。
 */
@RestController
@RequestMapping("/api")
@Profile("!prod")
public class DevAuthController {

    private final VerificationService verificationService;
    private final CreditApi creditApi;

    public DevAuthController(VerificationService verificationService, CreditApi creditApi) {
        this.verificationService = verificationService;
        this.creditApi = creditApi;
    }

    /** dev-only：一键把自己最新 PENDING 认证置 APPROVED。 */
    @PostMapping("/auth/verifications/me/dev-approve")
    public ApiResponse<VerificationStatusVO> devApprove() {
        long userId = CurrentUserHolder.getUserId();
        return ApiResponse.success(verificationService.devApproveMine(userId));
    }

    /**
     * dev-only：给自己补 100 启动积分（兼容 P2.7 改动前已注册的老用户）。
     * bizKey 用 user:N:signup，与 register 流程相同，幂等：
     * 老用户没领过 → 进账；P2.7 后注册的新用户已经领过 → 静默短路返回。
     */
    @PostMapping("/credit/me/dev-signup-bonus")
    public ApiResponse<Void> devSignupBonus() {
        long userId = CurrentUserHolder.getUserId();
        creditApi.award(userId, 100, "SIGNUP_BONUS", "user:" + userId + ":signup");
        return ApiResponse.success();
    }
}
