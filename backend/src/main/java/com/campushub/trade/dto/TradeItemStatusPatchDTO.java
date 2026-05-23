package com.campushub.trade.dto;

import com.campushub.trade.entity.TradeItemStatus;
import jakarta.validation.constraints.NotNull;

public class TradeItemStatusPatchDTO {

    @NotNull
    private TradeItemStatus status;

    public TradeItemStatus getStatus() { return status; }
    public void setStatus(TradeItemStatus status) { this.status = status; }
}
