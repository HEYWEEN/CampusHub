package com.campushub.trade.event;

/**
 * 二手订单双方确认完成事件（供 credit / notify listener 订阅）。
 */
public record TradeOrderCompletedEvent(
        Long orderId,
        Long buyerId,
        Long sellerId,
        Integer pointAmount
) {}
