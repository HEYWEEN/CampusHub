package com.campushub.user.service;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.PublicUserVO;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.response.ResponseCode;
import com.campushub.common.util.AesUtil;
import com.campushub.common.util.PhoneMaskUtil;
import com.campushub.user.api.UserApi;
import com.campushub.user.dto.PrivacyPatchDTO;
import com.campushub.user.dto.ProfilePatchDTO;
import com.campushub.user.entity.UserAuditLog;
import com.campushub.user.entity.UserAuditLog.AuditAction;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserAuditLogRepository;
import com.campushub.user.repository.UserProfileRepository;
import com.campushub.user.vo.UserMeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User 模块核心服务 —— 同时实现 UserApi 给跨模块用。
 *
 * 设计取舍：
 *   - **同一个 Bean 实现 UserService（模块内）+ UserApi（跨模块）** —— P3 §1 推荐的"内外双契约"
 *   - 这样跨模块调用方拿到的是 interface，不会反向依赖 user 模块的实现细节
 *   - 模块内调用走具体类拿到全 API；跨模块调用方只能看到 interface 子集
 */
@Service
public class UserService implements UserApi {

    private final UserProfileRepository profileRepo;
    private final AuthUserRepository userRepo;
    private final UserAuditLogRepository auditRepo;
    private final SensitiveWordChecker sensitive;
    private final AesUtil aes;

    public UserService(UserProfileRepository profileRepo,
                       AuthUserRepository userRepo,
                       UserAuditLogRepository auditRepo,
                       SensitiveWordChecker sensitive,
                       AesUtil aes) {
        this.profileRepo = profileRepo;
        this.userRepo = userRepo;
        this.auditRepo = auditRepo;
        this.sensitive = sensitive;
        this.aes = aes;
    }

    // ==================== UserApi 跨模块接口 ====================

    @Override
    @Transactional(readOnly = true)
    public PublicUserVO getPublicUser(long userId) {
        AuthUser u = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        UserProfile p = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("用户资料缺失: " + userId));
        return new PublicUserVO(
                u.getId(),
                p.getNickname(),
                p.getAvatarUrl(),
                u.getVerifyStatus() == VerifyStatus.APPROVED ? "校园已认证" : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VerifyStatus getVerifyStatus(long userId) {
        return userRepo.findById(userId)
                .map(AuthUser::getVerifyStatus)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(long userId) {
        return userRepo.existsById(userId);
    }

    // ==================== 模块内业务 ====================

    /**
     * USER-04 个人主页全字段（仅本人）。
     */
    @Transactional(readOnly = true)
    public UserMeVO getMe(long userId) {
        AuthUser u = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        UserProfile p = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("用户资料缺失: " + userId));

        // 解密手机号 → 脱敏返回；密文/明文都不下发到学生端
        String phoneMasked;
        try {
            phoneMasked = PhoneMaskUtil.mask(aes.decrypt(u.getPhoneCipher()));
        } catch (Exception ex) {
            phoneMasked = null; // 加密失效不阻塞主页查询
        }

        return new UserMeVO(
                u.getId(),
                p.getNickname(),
                p.getAvatarUrl(),
                phoneMasked,
                u.getVerifyStatus(),
                p.isHidePublishHistory(), p.isHideAcceptHistory(), p.isHideCourseReviews(),
                p.getDailyAcceptLimit()
        );
    }

    /**
     * USER-01 改昵称 / 头像。
     * 命中敏感词 → 400；写审计日志（仅 nickname 变化时）。
     */
    @Transactional
    public UserMeVO updateProfile(long userId, ProfilePatchDTO dto, String ip) {
        UserProfile p = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("用户资料缺失: " + userId));

        if (dto.getNickname() != null) {
            String hit = sensitive.firstHit(dto.getNickname());
            if (hit != null) {
                throw new BizException(ResponseCode.BAD_REQUEST,
                        "昵称含敏感词: " + hit);
            }
            String before = p.getNickname();
            if (!dto.getNickname().equals(before)) {
                p.setNickname(dto.getNickname());
                auditRepo.save(new UserAuditLog(userId, AuditAction.NICKNAME_CHANGE,
                        before, dto.getNickname(), ip));
            }
        }

        if (dto.getAvatarUrl() != null) {
            String before = p.getAvatarUrl();
            if (!dto.getAvatarUrl().equals(before == null ? "" : before)) {
                p.setAvatarUrl(dto.getAvatarUrl().isEmpty() ? null : dto.getAvatarUrl());
                auditRepo.save(new UserAuditLog(userId, AuditAction.AVATAR_CHANGE,
                        before, dto.getAvatarUrl(), ip));
            }
        }

        profileRepo.save(p);
        return getMe(userId);
    }

    /**
     * USER-02 三项隐私开关，每次变更写审计。
     */
    @Transactional
    public UserMeVO updatePrivacy(long userId, PrivacyPatchDTO dto, String ip) {
        UserProfile p = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("用户资料缺失: " + userId));

        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        boolean changed = false;

        if (dto.getHidePublishHistory() != null
                && dto.getHidePublishHistory() != p.isHidePublishHistory()) {
            before.append("publish=").append(p.isHidePublishHistory()).append(";");
            after.append("publish=").append(dto.getHidePublishHistory()).append(";");
            p.setHidePublishHistory(dto.getHidePublishHistory());
            changed = true;
        }
        if (dto.getHideAcceptHistory() != null
                && dto.getHideAcceptHistory() != p.isHideAcceptHistory()) {
            before.append("accept=").append(p.isHideAcceptHistory()).append(";");
            after.append("accept=").append(dto.getHideAcceptHistory()).append(";");
            p.setHideAcceptHistory(dto.getHideAcceptHistory());
            changed = true;
        }
        if (dto.getHideCourseReviews() != null
                && dto.getHideCourseReviews() != p.isHideCourseReviews()) {
            before.append("courseReviews=").append(p.isHideCourseReviews()).append(";");
            after.append("courseReviews=").append(dto.getHideCourseReviews()).append(";");
            p.setHideCourseReviews(dto.getHideCourseReviews());
            changed = true;
        }

        if (changed) {
            profileRepo.save(p);
            auditRepo.save(new UserAuditLog(userId, AuditAction.PRIVACY_TOGGLE,
                    before.toString(), after.toString(), ip));
        }
        return getMe(userId);
    }
}
