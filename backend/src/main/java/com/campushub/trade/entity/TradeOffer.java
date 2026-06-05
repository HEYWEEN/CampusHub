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

/**
 * 二手砍价单（单行 ping-pong）：买家发起，买卖双方轮流还价，
 * 任一方在自己的回合 accept → 复用 TradeOrder 成单。
 */
@Entity
@Table(
    name = "trade_offer",
    indexes = {
        @Index(name = "idx_trade_offer_buyer_status", columnList = "buyer_id, status"),
        @Index(name = "idx_trade_offer_seller_status", columnList = "seller_id, status"),
        @Index(name = "idx_trade_offer_item_id", columnList = "item_id")
    }
)
public class TradeOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "price_point", nullable = false)
    private int pricePoint;

    @Column(name = "status", nullable = false)
    private TradeOfferStatus status = TradeOfferStatus.PENDING;

    @Column(name = "awaiting_party", nullable = false)
    private AwaitingParty awaitingParty = AwaitingParty.SELLER;

    @Column(name = "order_id")
    private Long orderId;

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

    public TradeOffer() {}

    public TradeOffer(Long itemId, Long buyerId, Long sellerId, int pricePoint) {
        this.itemId = itemId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.pricePoint = pricePoint;
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
    public int getPricePoint() { return pricePoint; }
    public void setPricePoint(int pricePoint) {
        this.pricePoint = pricePoint;
        touch();
    }
    public TradeOfferStatus getStatus() { return status; }
    public void setStatus(TradeOfferStatus status) {
        this.status = status;
        touch();
    }
    public AwaitingParty getAwaitingParty() { return awaitingParty; }
    public void setAwaitingParty(AwaitingParty awaitingParty) {
        this.awaitingParty = awaitingParty;
        touch();
    }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
        touch();
    }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
