package com.campushub.credit.api;

/**
 * 跨模块信用/积分契约（P3 §5 跨模块接口表 + P4 §02 分工）。
 *
 * **任何业务模块（task / trade / report / search ...）想冻结积分、结算、查信用分**
 * 都必须 @Autowired 此 interface，禁止直接 @Autowired CreditServiceImpl。
 *
 * <h3>幂等模型（重要）</h3>
 * 采用 <b>bizKey 幂等</b>，不是 freezeId 句柄模型。每次资金操作传入一个全局唯一的
 * {@code bizKey}，落到 {@code credit_record.biz_id}（唯一键 uk_credit_record_biz）。
 * 重复传同一个 bizKey 视为同一操作，第二次静默短路、不重复扣加，保证事件重投/重试安全。
 *
 * <h3>bizKey 约定格式</h3>
 * {@code <module>:<entityId>:<operation>}，例如：
 * <ul>
 *   <li>{@code task:123:freeze}   —— 发布任务冻结悬赏</li>
 *   <li>{@code task:123:settle}   —— 任务完成结算给接单者</li>
 *   <li>{@code task:123:unfreeze} —— 任务取消退冻</li>
 *   <li>{@code trade:456:freeze}  —— 二手下单冻结买家积分</li>
 * </ul>
 *
 * <h3>铁律</h3>
 * point_balance / point_frozen / credit_score 在任何路径都不出负；credit_score 夹紧 [0,120]。
 */
public interface CreditApi {

    /**
     * 查用户当前信用分（0~120）。
     * 用于 task 发布/接单的信用闸门（&lt;60 禁发禁接）、search 阈值过滤、admin 仲裁。
     * 账户不存在时按"已认证初始 100"语义懒创建后返回。
     */
    int getScoreOf(long userId);

    /**
     * 冻结积分：从 point_balance 扣除并锁入 point_frozen。
     * 用于发布任务冻结悬赏、二手下单冻结买家积分、申诉押金等。
     * 余额不足抛 {@code BizException(CreditErrorCode.CREDIT_NOT_ENOUGH)}。
     *
     * @param userId 被冻结积分的用户
     * @param points 冻结数量（&gt;0）
     * @param bizKey 幂等键，如 {@code task:123:freeze}
     */
    void freeze(long userId, int points, String bizKey);

    /**
     * 解冻积分：从 point_frozen 退回 point_balance（不转移给第三方）。
     * 用于任务/订单取消。
     *
     * @param bizKey 幂等键，如 {@code task:123:unfreeze}
     */
    void unfreeze(long userId, int points, String bizKey);

    /**
     * 结算积分：把冻结的积分从付款方转移给收款方（完成交易）。
     * 用于任务完成结算、二手订单完成。
     *
     * @param userId 收款方（如任务接单者）
     * @param points 结算数量
     * @param bizKey 幂等键，如 {@code task:123:settle}
     */
    void settle(long userId, int points, String bizKey);

    /**
     * 信用分扣减（走 Strategy 按 reasonCode 计算 delta），并写 credit_score_log。
     * 用于差评、爽约、申诉失败、严重违规、违禁词等场景，多由 listener / 仲裁触发。
     * 结果夹紧到 [0,120]，永不出负。
     *
     * @param delta      期望变化量（负数为扣分）；实际以 reasonCode 对应策略为准
     * @param reasonCode 策略选择器，如 BAD_REVIEW / TASK_NO_SHOW / APPEAL_FAILED / SEVERE_VIOLATION / FORBIDDEN_WORD
     * @param bizKey     幂等键，如 {@code review:999:bad}
     */
    void deduct(long userId, int delta, String reasonCode, String bizKey);
}
