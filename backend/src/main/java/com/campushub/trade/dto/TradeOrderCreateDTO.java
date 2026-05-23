package com.campushub.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TradeOrderCreateDTO {

    @NotNull
    private Long itemId;

    @Min(1)
    private int negotiatedPricePoint;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public int getNegotiatedPricePoint() { return negotiatedPricePoint; }
    public void setNegotiatedPricePoint(int negotiatedPricePoint) { this.negotiatedPricePoint = negotiatedPricePoint; }
}
