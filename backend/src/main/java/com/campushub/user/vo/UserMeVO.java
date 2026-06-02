package com.campushub.user.vo;

import com.campushub.auth.entity.Role;
import com.campushub.common.enums.VerifyStatus;

/**
 * GET /api/users/me  全字段返回（仅本人）。
 *
 * 包含：
 *   - 基础资料（nickname / avatarUrl）
 *   - 三项隐私开关
 *   - 接单上限（task TASK-08 会更新此值）
 *   - 实名认证状态
 *   - 手机号脱敏字符串（不含明文）
 *
 * **不含**：phone 明文 / phone_cipher / password_hash / 任何 cipher 字段。
 */
public class UserMeVO {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneMasked;
    private VerifyStatus verifyStatus;

    private boolean hidePublishHistory;
    private boolean hideAcceptHistory;
    private boolean hideCourseReviews;

    private int dailyAcceptLimit;
    private Role role = Role.USER;

    public UserMeVO() {}

    public UserMeVO(Long userId, String nickname, String avatarUrl, String phoneMasked,
                    VerifyStatus verifyStatus,
                    boolean hidePublishHistory, boolean hideAcceptHistory, boolean hideCourseReviews,
                    int dailyAcceptLimit) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.phoneMasked = phoneMasked;
        this.verifyStatus = verifyStatus;
        this.hidePublishHistory = hidePublishHistory;
        this.hideAcceptHistory = hideAcceptHistory;
        this.hideCourseReviews = hideCourseReviews;
        this.dailyAcceptLimit = dailyAcceptLimit;
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPhoneMasked() { return phoneMasked; }
    public VerifyStatus getVerifyStatus() { return verifyStatus; }
    public boolean isHidePublishHistory() { return hidePublishHistory; }
    public boolean isHideAcceptHistory() { return hideAcceptHistory; }
    public boolean isHideCourseReviews() { return hideCourseReviews; }
    public int getDailyAcceptLimit() { return dailyAcceptLimit; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
