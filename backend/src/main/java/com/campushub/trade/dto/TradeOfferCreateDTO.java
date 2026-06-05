package com.campushub.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** POST /api/trade/offers — 买家发起砍价。 */
public class TradeOfferCreateDTO {

    @NotNull
    private Long itemId;

    @Min(1)
    private int pricePoint;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public int getPricePoint() { return pricePoint; }
    public void setPricePoint(int pricePoint) { this.pricePoint = pricePoint; }
}
