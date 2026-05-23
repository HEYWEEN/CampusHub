package com.campushub.trade.vo;

import com.campushub.trade.entity.TradeOrderStatus;

import java.time.Instant;

public record TradeOrderVO(
        Long id,
        Long itemId,
        Long buyerId,
        Long sellerId,
        TradeOrderStatus status,
        int negotiatedPricePoint,
        int freezePoint,
        boolean buyerConfirmed,
        boolean sellerConfirmed,
        Instant createdAt
) {}
