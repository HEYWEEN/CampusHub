package com.campushub.credit.listener;

import com.campushub.credit.api.CreditApi;
import com.campushub.task.event.TaskCompletedEvent;
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
 * <p><b>已知设计缺口（非本 PR 修，对齐 {@link TradeEventListener} javadoc）</b>：
 * <ul>
 *   <li>{@code settle} 当前是「收款方入账」单边语义，配合 {@code unfreeze}（退回 balance）等价于
 *       「发布者拿回悬赏 + 接单者额外入账」，全局非守恒。等价于 P3 / CreditServiceImpl line 38 注释中
 *       提到的「付款方冻结释放」尚未定稿。推后到 CreditApi 增加
 *       {@code transfer(payerId, payeeId, points, bizKey)} 时一并修复。</li>
 *   <li>接单者押金（{@code task:<id>:deposit}，accept 时冻结）在完成路径上 <b>不</b> 被本 listener 释放，
 *       因为 {@link TaskCompletedEvent} payload 未携带 {@code depositPoint}（仅 cancel 事件携带）。
 *       押金返还应由 task 模块在 confirm 内联处理，或等事件 payload 扩展后再补到 listener。</li>
 * </ul>
 */
@Component("creditTaskEventListener")
public class TaskEventListener {

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
}
