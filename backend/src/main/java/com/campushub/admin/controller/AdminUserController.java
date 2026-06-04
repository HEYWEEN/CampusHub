package com.campushub.admin.controller;

import com.campushub.admin.dto.AdminBanDTO;
import com.campushub.admin.dto.AdminRoleDTO;
import com.campushub.admin.service.AdminUserService;
import com.campushub.admin.vo.AdminUserVO;
import com.campushub.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/admin/users/* —— 用户搜索 + 封禁/解封（F-ADMIN-02）。 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /** 按 userId（纯数字）或昵称模糊搜索。 */
    @GetMapping
    public ApiResponse<List<AdminUserVO>> search(@RequestParam(required = false) String q) {
        return ApiResponse.success(adminUserService.search(q));
    }

    @PatchMapping("/{id}/ban")
    public ApiResponse<AdminUserVO> ban(@PathVariable("id") long userId,
                                         @Valid @RequestBody AdminBanDTO dto) {
        return ApiResponse.success(adminUserService.setBan(userId, dto.getBanned(), dto.getReason()));
    }

    /** 分派 / 撤销管理员：仅超级管理员可调用（service 内部校验调用者）。 */
    @PatchMapping("/{id}/role")
    public ApiResponse<AdminUserVO> setRole(@PathVariable("id") long userId,
                                            @Valid @RequestBody AdminRoleDTO dto) {
        return ApiResponse.success(adminUserService.setAdminRole(userId, dto.getAdmin()));
    }
}
