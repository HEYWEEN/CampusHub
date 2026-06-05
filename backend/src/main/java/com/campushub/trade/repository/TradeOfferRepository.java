package com.campushub.trade.repository;

import com.campushub.trade.entity.TradeOffer;
import com.campushub.trade.entity.TradeOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeOfferRepository extends JpaRepository<TradeOffer, Long> {

    /** 应用层防重：同一买家对同一商品最多一条进行中报价。 */
    boolean existsByItemIdAndBuyerIdAndStatus(Long itemId, Long buyerId, TradeOfferStatus status);

    /** 「我的议价」：买家或卖家是我的全部报价，最近活动优先。 */
    List<TradeOffer> findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(Long buyerId, Long sellerId);
}
