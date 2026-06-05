package com.campushub.user.controller;

import com.campushub.auth.service.AuthService;
import com.campushub.common.PublicUserVO;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.common.util.IpUtil;
import com.campushub.user.dto.ChangePasswordDTO;
import com.campushub.user.dto.PrivacyPatchDTO;
import com.campushub.user.dto.ProfilePatchDTO;
import com.campushub.user.service.MeStatsService;
import com.campushub.user.service.UserService;
import com.campushub.user.vo.MeStatsVO;
import com.campushub.user.vo.PublicUserStatsVO;
import com.campushub.user.vo.UserMeVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/users/* —— 用户资料、隐私、公开主页（USER-01 ~ USER-04）
 *
 * 路径设计（与 P3 §2.1 路径公约 + OpenAPI 对齐）：
 *   GET    /api/users/me                  本人全字段
 *   PATCH  /api/users/me/profile          改昵称/头像
 *   PATCH  /api/users/me/privacy          改三项隐私开关
 *   GET    /api/users/{userId}/public     公开主页（仅 PublicUserVO）
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final MeStatsService meStatsService;
    private final AuthService authService;

    public UserController(UserService userService, MeStatsService meStatsService, AuthService authService) {
        this.userService = userService;
        this.meStatsService = meStatsService;
        this.authService = authService;
    }

    /** USER-04 个人主页（仅本人） */
    @GetMapping("/me")
    public ApiResponse<UserMeVO> getMe() {
        long userId = CurrentUserHolder.getUserId();
        return ApiResponse.success(userService.getMe(userId));
    }

    /** USER-04 个人主页三项统计（仅本人，真实计数，不受隐私开关影响） */
    @GetMapping("/me/stats")
    public ApiResponse<MeStatsVO> getMyStats() {
        long userId = CurrentUserHolder.getUserId();
        return ApiResponse.success(meStatsService.getMyStats(userId));
    }

    /** USER-01 改资料：昵称（敏感词 400）/ 头像 */
    @PatchMapping("/me/profile")
    public ApiResponse<UserMeVO> updateProfile(@Valid @RequestBody ProfilePatchDTO dto,
                                                HttpServletRequest request) {
        long userId = CurrentUserHolder.getUserId();
        String ip = IpUtil.resolve(request);
        return ApiResponse.success(userService.updateProfile(userId, dto, ip));
    }

    /** USER-02 隐私开关（默认开 + 变更写 user_audit_log） */
    @PatchMapping("/me/privacy")
    public ApiResponse<UserMeVO> updatePrivacy(@Valid @RequestBody PrivacyPatchDTO dto,
                                                HttpServletRequest request) {
        long userId = CurrentUserHolder.getUserId();
        String ip = IpUtil.resolve(request);
        return ApiResponse.success(userService.updatePrivacy(userId, dto, ip));
    }

    /** 登录态设置 / 修改密码：已有密码需传 oldPassword 校验。 */
    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        authService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return ApiResponse.success(null);
    }

    /** USER-03 公开主页（任意人可查，但只返回 PublicUserVO） */
    @GetMapping("/{userId}/public")
    public ApiResponse<PublicUserVO> getPublic(@PathVariable long userId) {
        return ApiResponse.success(userService.getPublicUser(userId));
    }

    /**
     * 公开主页 + 三项可隐藏统计（schema_audit A-10 修复）。
     * 真实跨模块聚合（task / review）+ 按主页主人的隐私开关逐项 mask：
     * 被隐藏的项返回 null（前端渲染「已隐藏」）。「查看公开主页」即预览外人所见，
     * 故自己看自己同样受隐私开关约束。
     */
    @GetMapping("/{userId}/public/stats")
    public ApiResponse<PublicUserStatsVO> getPublicStats(@PathVariable long userId) {
        UserService.PublicStatsContext ctx = userService.getPublicStatsContext(userId);
        return ApiResponse.success(meStatsService.getPublicStats(
                ctx.user(), userId,
                ctx.hidePublished(), ctx.hideAccepted(), ctx.hideReviews()));
    }
}
