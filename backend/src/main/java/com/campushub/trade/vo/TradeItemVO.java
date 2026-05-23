package com.campushub.trade.vo;

import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeItemStatus;

import java.time.Instant;
import java.util.List;

public record TradeItemVO(
        Long id,
        Long sellerId,
        String title,
        String description,
        int pricePoint,
        PickupLocationType pickupLocationType,
        String pickupLocationDetail,
        TradeItemStatus status,
        List<String> imageUrls,
        Instant createdAt
) {}
