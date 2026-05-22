package com.campushub.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * user_profile：用户资料 + 三项隐私开关（P3 schema.sql user_profile）。
 *
 * 与 auth_user 是 1:1 逻辑关联（user_id → auth_user.id），
 * **不加 DB 级外键**（P3 §1 数据库公约：禁止跨模块 FK）。
 *
 * 三项隐私开关默认 ON（隐藏），P3 §1 隐私默认隐藏；
 * 变更时由 USER-02 写入 user_audit_log。
 */
@Entity
@Table(
    name = "user_profile",
    indexes = {
        @Index(name = "uk_user_profile_user_id", columnList = "user_id", unique = true)
    }
)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "nickname", length = 64, nullable = false)
    private String nickname;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "hide_publish_hist", nullable = false)
    private boolean hidePublishHistory = true;

    @Column(name = "hide_accept_hist", nullable = false)
    private boolean hideAcceptHistory = true;

    @Column(name = "hide_course_reviews", nullable = false)
    private boolean hideCourseReviews = true;

    /** 个人接单上限 1~3，默认 2（P3 user_profile.daily_accept_limit；task 模块 TASK-08 维护） */
    @Column(name = "daily_accept_limit", nullable = false)
    private int dailyAcceptLimit = 2;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserProfile() {}

    public UserProfile(Long userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = Instant.now();
    }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }
    public boolean isHidePublishHistory() { return hidePublishHistory; }
    public void setHidePublishHistory(boolean v) { this.hidePublishHistory = v; this.updatedAt = Instant.now(); }
    public boolean isHideAcceptHistory() { return hideAcceptHistory; }
    public void setHideAcceptHistory(boolean v) { this.hideAcceptHistory = v; this.updatedAt = Instant.now(); }
    public boolean isHideCourseReviews() { return hideCourseReviews; }
    public void setHideCourseReviews(boolean v) { this.hideCourseReviews = v; this.updatedAt = Instant.now(); }
    public int getDailyAcceptLimit() { return dailyAcceptLimit; }
    public void setDailyAcceptLimit(int dailyAcceptLimit) {
        this.dailyAcceptLimit = dailyAcceptLimit;
        this.updatedAt = Instant.now();
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
