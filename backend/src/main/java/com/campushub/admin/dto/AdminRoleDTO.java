package com.campushub.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * PATCH /api/admin/users/{id}/role —— 分派 / 撤销管理员（仅超级管理员可调用）。
 * admin=true 设为 ADMIN，false 还原为 USER。
 */
public class AdminRoleDTO {

    @NotNull
    private Boolean admin;

    public Boolean getAdmin() { return admin; }
    public void setAdmin(Boolean admin) { this.admin = admin; }
}
