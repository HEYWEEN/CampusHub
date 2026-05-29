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
    name = "trade_item",
    indexes = {
        @Index(name = "idx_trade_item_seller_status", columnList = "seller_id, status"),
        @Index(name = "idx_trade_item_created_at", columnList = "created_at")
    }
)
public class TradeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "price_point", nullable = false)
    private int pricePoint;

    @Column(name = "pickup_location_type", nullable = false)
    private PickupLocationType pickupLocationType = PickupLocationType.BUILDING_RANGE;

    @Column(name = "pickup_location_detail", length = 200)
    private String pickupLocationDetail;

    @Column(name = "status", nullable = false)
    private TradeItemStatus status = TradeItemStatus.ON_SALE;

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

    public TradeItem() {}

    public TradeItem(Long sellerId, String title, String description, int pricePoint,
                     PickupLocationType pickupLocationType, String pickupLocationDetail) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.pricePoint = pricePoint;
        this.pickupLocationType = pickupLocationType;
        this.pickupLocationDetail = pickupLocationDetail;
        this.creatorId = sellerId;
        this.updaterId = sellerId;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getPricePoint() { return pricePoint; }
    public PickupLocationType getPickupLocationType() { return pickupLocationType; }
    public String getPickupLocationDetail() { return pickupLocationDetail; }
    public TradeItemStatus getStatus() { return status; }
    public void setStatus(TradeItemStatus status) {
        this.status = status;
        touch();
    }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
