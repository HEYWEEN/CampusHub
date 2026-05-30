package com.campushub.credit.listener;

import com.campushub.credit.api.CreditApi;
import com.campushub.task.event.TaskCompletedEvent;
import com.campushub.task.event.TaskExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅 {@link TaskCompletedEvent}，把 task 完成事件转化为 credit 资金流（CRD-04，task 段）。
 *
 * <p><b>调用序列</b>（看板 §1.5 CRD-04「约定走 unfreeze + settle 两段调用以适配单 userId 的 settle 契约」）：
 * <ol>
 *   <li>{@code unfreeze(publisherId, reward, "task:<id>:settle_unfreeze")} —— 释放发布者发布时冻结的悬赏</li>
 *   <li>{@code settle(accepterId,    reward, "task:<id>:settle")}          —— 给接单者入账</li>
 * </ol>
 *
 * <p><b>幂等</b>：两个 bizKey 后缀不同（{@code :settle_unfreeze} / {@code :settle}），命中
 * {@code uk_credit_record_biz} 即静默短路；事件重投（同一 taskId 二次触发）不会重复扣加。
 *
 * <p><b>事务</b>：默认 {@code @EventListener} 同步执行，且 {@link CreditApi} 实现是
 * {@code @Transactional}（REQUIRED），与发布者（TaskServiceImpl.confirmComplete）的 TX
 * 合并 —— 任一段失败则确认完成整体回滚。
 *
 * <p><b>为什么不订阅 {@link com.campushub.task.event.TaskCanceledEvent}</b>：
 * 取消路径 {@code TaskServiceImpl.cancel}（lines 219/222）已经 <i>内联</i>调用 {@code unfreeze}
 * 退还发布者悬赏 + 接单者押金（bizKey {@code task:<id>:unfreeze_reward} / {@code task:<id>:unfreeze_deposit}）。
 * 若 listener 再 unfreeze 一次会因 bizKey 后缀不同绕过幂等键，造成双倍退款。事件本身留给 notify 模块消费。
 *
 * <p><b>为什么订阅 {@link TaskExpiredEvent} 但什么都不做（显式 noop）</b>：
 * 任务过期时（{@code TaskTimeoutScanner} 触发），看板 §1.3 TASK-07 把这条路径标注为「进入仲裁队列」。
 * 仲裁模块（admin/arbitration）尚未落地，**现阶段无法判断**冻结积分应该退还还是没收 ——
 * <ul>
 *   <li>publisher 的悬赏：若 accepter 未提交凭证 → 退还 publisher；若提交凭证但 publisher 拖延 → 应结算给 accepter</li>
 *   <li>accepter 的押金：取决于过期归因，仲裁裁定后才能定向</li>
 * </ul>
 * 选择 <b>显式 noop + 日志</b>，而不是「不订阅」，因为：
 * <ol>
 *   <li>「不订阅」会在 grep 时让未来读者误以为 credit 完全不关心 expired 事件；显式 noop 留下钩子。</li>
 *   <li>future-proof：仲裁模块来了，加逻辑只需改本方法体，不用重新探路事件订阅位。</li>
 *   <li>noop 行为本身在单测里被固定（{@code onTaskExpired_isNoop}），防止有人未来「顺手」加错副作用。</li>
 * </ol>
 * <b>当前副作用</b>：publisher 的 reward 和 accepter 的 deposit 永远停留在 {@code point_frozen}，
 * 直到仲裁/手工运维介入。这是已知缺口，与「完成路径上 deposit 不退」同套路（见上一条），
 * 集中登记在 {@code docs/P4/06_后端代码审查指南.md §5 已知技术债}。
 *
 * <p><b>已知设计缺口（非本 PR 修，对齐 {@link TradeEventListener} javadoc）</b>：
 * <ul>
 *   <li>{@code settle} 当前是「收款方入账」单边语义，配合 {@code unfreeze}（退回 balance）等价于
 *       「发布者拿回悬赏 + 接单者额外入账」，全局非守恒。等价于 P3 / CreditServiceImpl line 38 注释中
 *       提到的「付款方冻结释放」尚未定稿。推后到 CreditApi 增加
 *       {@code transfer(payerId, payeeId, points, bizKey)} 时一并修复。</li>
 *   <li>接单者押金（{@code task:<id>:deposit}，accept 时冻结）在完成路径上 <b>不</b> 被本 listener 释放，
 *       因为 {@link TaskCompletedEvent} payload 未携带 {@code depositPoint}（仅 cancel 事件携带）。
 *       押金返还应由 task 模块在 confirm 内联处理，或等事件 payload 扩展后再补到 listener。</li>
 *   <li>过期路径（{@link TaskExpiredEvent}）冻结积分无人退还，等仲裁模块定稿（见上方 noop 说明）。</li>
 * </ul>
 */
@Component("creditTaskEventListener")
public class TaskEventListener {

    private static final Logger log = LoggerFactory.getLogger(TaskEventListener.class);

    private final CreditApi creditApi;

    public TaskEventListener(CreditApi creditApi) {
        this.creditApi = creditApi;
    }

    @EventListener
    @Transactional
    public void onTaskCompleted(TaskCompletedEvent event) {
        String prefix = "task:" + event.taskId();
        creditApi.unfreeze(event.publisherId(), event.rewardPoint(), prefix + ":settle_unfreeze");
        creditApi.settle  (event.accepterId(),  event.rewardPoint(), prefix + ":settle");
    }

    /**
     * 任务过期 —— 当前显式 noop，等仲裁模块定稿。详见类头 javadoc「为什么订阅但什么都不做」。
     */
    @EventListener
    public void onTaskExpired(TaskExpiredEvent event) {
        log.info("task expired (credit no-op, awaiting arbitration): taskId={}, publisherId={}, accepterId={}",
                event.taskId(), event.publisherId(), event.accepterId());
    }
}
