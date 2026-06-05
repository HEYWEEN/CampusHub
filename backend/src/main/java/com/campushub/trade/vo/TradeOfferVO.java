package com.campushub.trade.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.trade.entity.TradeOffer;
import com.campushub.trade.entity.TradeOfferStatus;

import java.time.Instant;

/**
 * 砍价 VO（带商品快照 + 双方公开信息）。
 *
 * <p>{@code myTurn} 是前端按钮显隐的关键：仅当前 awaiting 方能 同意/还价/拒绝。
 */
public record TradeOfferVO(
        Long id,
        Long itemId,
        String itemTitle,
        int itemPricePoint,
        PublicUserVO buyer,
        PublicUserVO seller,
        int pricePoint,
        TradeOfferStatus status,
        String awaitingRole,   // "BUYER" | "SELLER"
        boolean isBuyer,       // viewer == buyerId
        boolean myTurn,        // viewer 是当前 awaiting 方
        Long orderId,
        Instant createdAt,
        Instant updatedAt
) {
    public static TradeOfferVO from(TradeOffer o, String itemTitle, int itemPricePoint,
                                    PublicUserVO buyer, PublicUserVO seller, long viewerId) {
        boolean isBuyer = o.getBuyerId() == viewerId;
        boolean myTurn = o.getStatus() == TradeOfferStatus.PENDING
                && (isBuyer
                    ? o.getAwaitingParty() == com.campushub.trade.entity.AwaitingParty.BUYER
                    : o.getAwaitingParty() == com.campushub.trade.entity.AwaitingParty.SELLER);
        return new TradeOfferVO(
                o.getId(),
                o.getItemId(),
                itemTitle,
                itemPricePoint,
                buyer,
                seller,
                o.getPricePoint(),
                o.getStatus(),
                o.getAwaitingParty().name(),
                isBuyer,
                myTurn,
                o.getOrderId(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
