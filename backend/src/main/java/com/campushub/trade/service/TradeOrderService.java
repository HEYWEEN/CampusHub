package com.campushub.trade.service;

import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.vo.TradeOrderVO;

import java.util.List;

public interface TradeOrderService {

    TradeOrderVO createOrder(long buyerId, TradeOrderCreateDTO dto);

    TradeOrderVO getOrder(long userId, long orderId);

    TradeOrderVO confirmOrder(long userId, long orderId);

    /** 「我的交易」：买家或卖家是我的全部订单。 */
    List<TradeOrderVO> myOrders(long userId);

    /** 取消订单（完成前，买卖任一方可取消）→ 退押金 + 商品回 ON_SALE。 */
    TradeOrderVO cancelOrder(long userId, long orderId);
}
