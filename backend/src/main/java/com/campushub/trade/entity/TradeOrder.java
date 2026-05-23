package com.campushub.trade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(
    name = "trade_order",
    indexes = {
        @Index(name = "idx_trade_order_buyer_status", columnList = "buyer_id, status"),
        @Index(name = "idx_trade_order_seller_status", columnList = "seller_id, status"),
        @Index(name = "idx_trade_order_item_id", columnList = "item_id")
    }
)
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "status", nullable = false)
    private TradeOrderStatus status = TradeOrderStatus.IN_TRADE;

    @Column(name = "negotiated_price_point", nullable = false)
    private int negotiatedPricePoint;

    @Column(name = "freeze_point", nullable = false)
    private int freezePoint;

    @Column(name = "buyer_confirmed", nullable = false)
    private boolean buyerConfirmed;

    @Column(name = "seller_confirmed", nullable = false)
    private boolean sellerConfirmed;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "updater_id")
    private Long updaterId;

    public TradeOrder() {}

    public TradeOrder(Long itemId, Long buyerId, Long sellerId, int negotiatedPricePoint) {
        this.itemId = itemId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.negotiatedPricePoint = negotiatedPricePoint;
        this.freezePoint = negotiatedPricePoint;
        this.creatorId = buyerId;
        this.updaterId = buyerId;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public Long getBuyerId() { return buyerId; }
    public Long getSellerId() { return sellerId; }
    public TradeOrderStatus getStatus() { return status; }
    public void setStatus(TradeOrderStatus status) {
        this.status = status;
        touch();
    }
    public int getNegotiatedPricePoint() { return negotiatedPricePoint; }
    public int getFreezePoint() { return freezePoint; }
    public boolean isBuyerConfirmed() { return buyerConfirmed; }
    public void setBuyerConfirmed(boolean buyerConfirmed) {
        this.buyerConfirmed = buyerConfirmed;
        touch();
    }
    public boolean isSellerConfirmed() { return sellerConfirmed; }
    public void setSellerConfirmed(boolean sellerConfirmed) {
        this.sellerConfirmed = sellerConfirmed;
        touch();
    }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
