package com.campushub.admin.vo;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.Role;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.user.entity.UserProfile;

/** 管理端用户行（含封禁/角色/认证态，仅 admin 端）。 */
public record AdminUserVO(
        Long userId,
        String nickname,
        String avatarUrl,
        VerifyStatus verifyStatus,
        boolean banned,
        Role role
) {
    public static AdminUserVO of(AuthUser u, UserProfile p) {
        return new AdminUserVO(
                u.getId(),
                p == null ? null : p.getNickname(),
                p == null ? null : p.getAvatarUrl(),
                u.getVerifyStatus(),
                u.isBanned(),
                u.getRole());
    }
}
