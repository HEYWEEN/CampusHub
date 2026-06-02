package com.campushub.team.exception;

/**
 * team 模块业务错误码（7xxx 段，2/4/5/6/9/10xxx 已被其它模块占用）。
 */
public final class TeamErrorCode {

    private TeamErrorCode() {}

    /** 非队长操作（看申请列表 / 审核） — 403 */
    public static final int NOT_CAPTAIN = 7001;

    /** 队伍已满员 — 409 */
    public static final int ALREADY_FULL = 7002;

    /** 不能申请自己发起的组队 — 400 */
    public static final int SELF_APPLY = 7003;

    /** 同帖 24h 申请超过 3 次 — 429 */
    public static final int APPLY_RATE_LIMIT = 7004;

    /** 申请不存在 / 已被处理（非 PENDING） — 409 */
    public static final int APPLICATION_NOT_PENDING = 7005;

    /** 招募已关闭 / 非招募中 — 409 */
    public static final int RECRUIT_CLOSED = 7006;
}
