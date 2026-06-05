package com.campushub.trade.service;

import com.campushub.trade.dto.TradeOfferCreateDTO;
import com.campushub.trade.dto.TradeOfferPriceDTO;
import com.campushub.trade.vo.TradeOfferVO;

import java.util.List;

/** 二手砍价（offer/counter-offer）服务。 */
public interface TradeOfferService {

    /** 买家发起报价。 */
    TradeOfferVO createOffer(long buyerId, TradeOfferCreateDTO dto);

    /** 当前回合方还价（改价 + 翻转回合）。 */
    TradeOfferVO counter(long userId, long offerId, TradeOfferPriceDTO dto);

    /** 当前回合方同意 → 复用 createOrder 成单。 */
    TradeOfferVO accept(long userId, long offerId);

    /** 当前回合方拒绝。 */
    TradeOfferVO reject(long userId, long offerId);

    /** 买家撤回自己的报价（任意进行中状态）。 */
    TradeOfferVO cancel(long userId, long offerId);

    /** 「我的议价」：买家或卖家是我的全部报价。 */
    List<TradeOfferVO> myOffers(long userId);
}
