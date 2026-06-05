package com.campushub.trade.dto;

import jakarta.validation.constraints.Min;

/** POST /api/trade/offers/{id}/counter — 还价新价格。 */
public class TradeOfferPriceDTO {

    @Min(1)
    private int pricePoint;

    public int getPricePoint() { return pricePoint; }
    public void setPricePoint(int pricePoint) { this.pricePoint = pricePoint; }
}
