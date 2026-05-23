package com.campushub.trade.dto;

import com.campushub.trade.entity.PickupLocationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TradeItemCreateDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @Min(1)
    private int pricePoint;

    @NotNull
    private PickupLocationType pickupLocationType = PickupLocationType.BUILDING_RANGE;

    @Size(max = 200)
    private String pickupLocationDetail;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPricePoint() { return pricePoint; }
    public void setPricePoint(int pricePoint) { this.pricePoint = pricePoint; }
    public PickupLocationType getPickupLocationType() { return pickupLocationType; }
    public void setPickupLocationType(PickupLocationType pickupLocationType) { this.pickupLocationType = pickupLocationType; }
    public String getPickupLocationDetail() { return pickupLocationDetail; }
    public void setPickupLocationDetail(String pickupLocationDetail) { this.pickupLocationDetail = pickupLocationDetail; }
}
