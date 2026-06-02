package com.campushub.admin.service;

import com.campushub.admin.vo.AdminUserVO;
import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.exception.NotFoundException;
import com.campushub.notify.api.NotifyApi;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 管理端用户管理：搜索 + 封禁/解封（F-ADMIN-02）。 */
@Service
public class AdminUserService {

    private final AuthUserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final NotifyApi notifyApi;

    public AdminUserService(AuthUserRepository userRepo, UserProfileRepository profileRepo, NotifyApi notifyApi) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.notifyApi = notifyApi;
    }

    /** q 为纯数字 → 按 userId 精确查；否则按昵称模糊。 */
    @Transactional(readOnly = true)
    public List<AdminUserVO> search(String q) {
        if (q == null || q.isBlank()) return List.of();
        String key = q.trim();
        List<AdminUserVO> out = new ArrayList<>();
        if (key.matches("\\d+")) {
            userRepo.findById(Long.parseLong(key)).ifPresent(u ->
                    out.add(AdminUserVO.of(u, profileRepo.findByUserId(u.getId()).orElse(null))));
        } else {
            for (UserProfile p : profileRepo.findTop20ByNicknameContainingIgnoreCaseOrderByUserIdAsc(key)) {
                userRepo.findById(p.getUserId()).ifPresent(u -> out.add(AdminUserVO.of(u, p)));
            }
        }
        return out;
    }

    @Transactional
    public AdminUserVO setBan(long userId, boolean banned, String reason) {
        AuthUser u = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        u.setBanned(banned);
        userRepo.save(u);
        String body = banned
                ? ("你的账号已被封禁" + (reason == null || reason.isBlank() ? "" : "：" + reason))
                : "你的账号已解封，可正常使用";
        notifyApi.appendLetter(userId, "ACCOUNT_BAN", banned ? "账号被封禁" : "账号已解封",
                body, "ACCOUNT_BAN:" + userId + ":" + (banned ? "1" : "0") + ":" + System.currentTimeMillis());
        return AdminUserVO.of(u, profileRepo.findByUserId(userId).orElse(null));
    }
}
