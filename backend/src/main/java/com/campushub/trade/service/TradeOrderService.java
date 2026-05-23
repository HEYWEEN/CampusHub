package com.campushub.trade.service;

import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.vo.TradeOrderVO;

public interface TradeOrderService {

    TradeOrderVO createOrder(long buyerId, TradeOrderCreateDTO dto);

    TradeOrderVO getOrder(long userId, long orderId);

    TradeOrderVO confirmOrder(long userId, long orderId);
}
