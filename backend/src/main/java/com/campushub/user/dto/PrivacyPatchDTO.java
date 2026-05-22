package com.campushub.user.dto;

/**
 * PATCH /api/users/me/privacy  请求体（三项开关，全部可选）。
 *
 * 用 Boolean 包装类（而非 boolean primitive）：
 *   - null = 本次不动该字段
 *   - true / false = 显式更新
 *
 * 默认三项都 ON（隐藏），所以"打开"只能从 ON → OFF 或 OFF → ON。
 */
public class PrivacyPatchDTO {

    private Boolean hidePublishHistory;
    private Boolean hideAcceptHistory;
    private Boolean hideCourseReviews;

    public Boolean getHidePublishHistory() { return hidePublishHistory; }
    public void setHidePublishHistory(Boolean v) { this.hidePublishHistory = v; }
    public Boolean getHideAcceptHistory() { return hideAcceptHistory; }
    public void setHideAcceptHistory(Boolean v) { this.hideAcceptHistory = v; }
    public Boolean getHideCourseReviews() { return hideCourseReviews; }
    public void setHideCourseReviews(Boolean v) { this.hideCourseReviews = v; }
}
