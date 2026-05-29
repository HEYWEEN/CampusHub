package com.campushub.trade.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeItemStatus;

import java.time.Instant;
import java.util.List;

/**
 * 二手商品列表/详情 VO（对前端 GET 接口）。
 *
 * seller 用嵌套 PublicUserVO（与 task.publisher 一致风格），前端无需再调
 * /api/users/{sellerId}/public 拿卖家信息。
 * （schema_audit B-9 修复）
 */
public record TradeItemVO(
        Long id,
        PublicUserVO seller,
        String title,
        String description,
        int pricePoint,
        PickupLocationType pickupLocationType,
        String pickupLocationDetail,
        TradeItemStatus status,
        List<String> imageUrls,
        Instant createdAt
) {}
