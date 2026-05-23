package com.campushub.trade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "trade_item_image",
    indexes = @Index(name = "idx_trade_item_image_item_id", columnList = "item_id")
)
public class TradeItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "url", length = 512, nullable = false)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public TradeItemImage() {}

    public TradeItemImage(Long itemId, String url, int sortOrder) {
        this.itemId = itemId;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getUrl() { return url; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
