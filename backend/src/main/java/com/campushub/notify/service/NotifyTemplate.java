package com.campushub.notify.service;

/**
 * 站内信模板枚举（NTF-02 listener 与 NTF-01 service 共用）。
 *
 * <p>每个 enum 项封装 title / body 模板。模板用位置参数 {0}，由
 * {@link java.text.MessageFormat} 渲染。args 约定第一个传 taskId（不查 task 表，避免跨模块耦合）。
 *
 * <p><b>设计取舍</b>：早期版本想用任务标题渲染，但那要求 listener @Autowired TaskRepository，
 * 破坏 P3 §3.4 模块边界（notify 不依赖 task 实现）。用 taskId 是 pragmatic choice：
 * 文案略不友好但 listener 保持零依赖；前端可基于 bizKey 跳转到任务详情补全语境。
 */
public enum NotifyTemplate {

    TASK_ACCEPTED(
            "你的任务已被接单",
            "任务 #{0} 已被接单，请保持联系方式畅通。"),

    TASK_COMPLETED(
            "任务已完成",
            "任务 #{0} 已完成结算，欢迎给对方一个评价。"),

    TASK_CANCELED(
            "任务已取消",
            "任务 #{0} 已取消，相关冻结积分会自动退回。"),

    TASK_EXPIRED(
            "任务已超时",
            "任务 #{0} 已超过截止时间，自动标记为过期。");

    private final String titleTemplate;
    private final String bodyTemplate;

    NotifyTemplate(String titleTemplate, String bodyTemplate) {
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
    }

    public String renderTitle(Object... args) {
        return args.length == 0 ? titleTemplate : java.text.MessageFormat.format(titleTemplate, args);
    }

    public String renderBody(Object... args) {
        return args.length == 0 ? bodyTemplate : java.text.MessageFormat.format(bodyTemplate, args);
    }

    /** 构造 bizKey：{type}:{taskId}:{userId}，与 NotifyApi 约定一致。 */
    public String bizKey(long taskId, long userId) {
        return name() + ":" + taskId + ":" + userId;
    }
}
