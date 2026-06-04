package com.campushub.admin.service;

import com.campushub.admin.vo.AdminUserVO;
import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.Role;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.response.ResponseCode;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.notify.api.NotifyApi;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /** q 空 → 列出全部用户（按 id 升序）；纯数字 → 按 userId 精确查；否则按昵称模糊。 */
    @Transactional(readOnly = true)
    public List<AdminUserVO> search(String q) {
        if (q == null || q.isBlank()) {
            // 资料一次性捞成 map，避免逐个查 profile 的 N+1（管理端低频、用户量有限，可接受）
            Map<Long, UserProfile> profiles = profileRepo.findAll().stream()
                    .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));
            return userRepo.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                    .map(u -> AdminUserVO.of(u, profiles.get(u.getId())))
                    .toList();
        }
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

    /**
     * 分派 / 撤销管理员。**仅超级管理员可调用**（普通管理员越权 → 403）。
     * 约束：不能改自己的角色；不能动其他超级管理员。
     */
    @Transactional
    public AdminUserVO setAdminRole(long targetUserId, boolean makeAdmin) {
        long callerId = CurrentUserHolder.getUserId();
        AuthUser caller = userRepo.findById(callerId)
                .orElseThrow(() -> new BizException(ResponseCode.FORBIDDEN, "需要管理员权限"));
        if (!caller.isSuperAdmin()) {
            throw new BizException(ResponseCode.FORBIDDEN, "仅超级管理员可分派管理员");
        }
        if (callerId == targetUserId) {
            throw new BizException(ResponseCode.BAD_REQUEST, "不能修改自己的角色");
        }

        AuthUser target = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + targetUserId));
        if (target.isSuperAdmin()) {
            throw new BizException(ResponseCode.BAD_REQUEST, "不能修改超级管理员的角色");
        }

        target.setRole(makeAdmin ? Role.ADMIN : Role.USER);
        userRepo.save(target);
        notifyApi.appendLetter(targetUserId, "ROLE_CHANGE",
                makeAdmin ? "你已被设为管理员" : "你的管理员权限已被撤销",
                makeAdmin ? "现在可以访问管理后台，请合规使用权限。" : "你的账号已恢复为普通用户。",
                "ROLE_CHANGE:" + targetUserId + ":" + (makeAdmin ? "1" : "0") + ":" + System.currentTimeMillis());
        return AdminUserVO.of(target, profileRepo.findByUserId(targetUserId).orElse(null));
    }
}
