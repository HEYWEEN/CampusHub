package com.campushub.credit.listener;

import com.campushub.credit.api.CreditApi;
import com.campushub.trade.event.TradeOrderCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅 {@link TradeOrderCompletedEvent}，把 trade 完成事件转化为 credit 资金流（CRD-04，trade 段）。
 *
 * <p><b>调用序列</b>（看板 §1.5 CRD-04「约定走 unfreeze + settle 两段调用以适配单 userId 的 settle 契约」）：
 * <ol>
 *   <li>{@code unfreeze(buyerId, points, "trade:<id>:unfreeze")} —— 释放买家下单时冻结的押金</li>
 *   <li>{@code settle(sellerId,  points, "trade:<id>:settle")}   —— 给卖家入账</li>
 * </ol>
 *
 * <p><b>幂等</b>：两个 bizKey 后缀不同（{@code :unfreeze} / {@code :settle}），命中
 * {@code uk_credit_record_biz} 即静默短路；事件重投（同一 orderId 二次触发）不会重复扣加。
 *
 * <p><b>事务</b>：默认 {@code @EventListener} 同步执行，且 {@link CreditApi} 实现是
 * {@code @Transactional}（REQUIRED），与发布者（TradeOrderServiceImpl.confirmOrder）的 TX
 * 合并 —— 任一段失败则订单状态变更整体回滚。
 *
 * <p><b>已知设计缺口（非本 PR 修）</b>：现 {@code settle} 是「收款方入账」单边语义，
 * 配合 {@code unfreeze} 实际效果是「买家拿回押金 + 卖家额外入账」，全局非守恒。
 * 等价于 P3 / CreditServiceImpl line 38 注释中提到的「付款方冻结释放」尚未定稿。
 * 推后到 CreditApi 增加 {@code transfer(payerId, payeeId, points, bizKey)} 时一并修复。
 */
@Component
public class TradeEventListener {

    private final CreditApi creditApi;

    public TradeEventListener(CreditApi creditApi) {
        this.creditApi = creditApi;
    }

    @EventListener
    @Transactional
    public void onTradeOrderCompleted(TradeOrderCompletedEvent event) {
        String prefix = "trade:" + event.orderId();
        creditApi.unfreeze(event.buyerId(),  event.pointAmount(), prefix + ":unfreeze");
        creditApi.settle  (event.sellerId(), event.pointAmount(), prefix + ":settle");
    }
}
