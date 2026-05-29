package com.campushub.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/users/me/profile  请求体。
 * 所有字段都是**可选**的（PATCH 局部更新语义），null = 不动；空字符串 = 业务报错。
 *
 * 服务端兜底校验：
 *   - nickname 命中敏感词 → 400
 *   - avatarUrl 不为 file:// / data: 等异常 schema（XSS 防护轻量层）
 */
public class ProfilePatchDTO {

    @Size(min = 2, max = 64, message = "昵称长度需在 2-64 之间")
    private String nickname;

    @Size(max = 512, message = "头像 URL 不能超过 512 字符")
    // 允许：空 / 自家图床相对路径 /uploads/... / http(s) / file://（兼容历史脚本）
    @Pattern(regexp = "^$|^/uploads/.*|^(https?|file)://.*", message = "头像 URL 协议非法")
    private String avatarUrl;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
