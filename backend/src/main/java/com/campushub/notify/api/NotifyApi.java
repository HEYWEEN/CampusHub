package com.campushub.notify.api;

/**
 * 跨模块通知契约（P3 §3.4 NotifyService.appendLetter / 看板 INF-06）。
 *
 * <p>外部模块（task / trade / report / admin ...）想给用户发站内信，
 * 应 @Autowired 本接口，禁止直接 @Autowired NotifyServiceImpl。
 * 当前 notify 内部 listener 也走同一入口，避免双写路径。
 *
 * <h3>幂等模型</h3>
 * 与 credit 同源：每次发信传入全局唯一 {@code bizKey}，落到 {@code notify_message.biz_key}
 * （唯一键 uk_notify_biz_key）。同 bizKey 重复调用静默短路。
 *
 * <h3>bizKey 约定格式</h3>
 * {@code <type>:<entityId>:<userId>}，例如：
 * <ul>
 *   <li>{@code TASK_ACCEPTED:123:7001} —— 任务 123 被接单，通知发布者 7001</li>
 *   <li>{@code TASK_COMPLETED:123:7002} —— 任务 123 完成，通知接单者 7002</li>
 * </ul>
 */
public interface NotifyApi {

    /**
     * 发一条站内信（同 bizKey 幂等）。
     *
     * @param userId  接收人
     * @param type    类型枚举名（TASK_ACCEPTED / TASK_COMPLETED / ...）
     * @param title   标题（≤128）
     * @param body    正文（≤512）
     * @param bizKey  幂等键
     */
    void appendLetter(long userId, String type, String title, String body, String bizKey);
}
