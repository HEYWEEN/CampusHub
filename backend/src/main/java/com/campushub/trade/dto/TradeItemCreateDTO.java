package com.campushub.trade.dto;

import com.campushub.trade.entity.PickupLocationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建二手商品 DTO（JSON）。
 * 图片由前端预先调 POST /api/uploads 上传拿到 url 列表，再放在 imageUrls 字段一起提交。
 * （schema_audit A-3 / A-4 修复：原本是 multipart，与前端 JSON 调用不匹配）
 */
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

    /** 由前端调 /api/uploads 拿到的图片 URL 列表（≤ 9 张） */
    @Size(max = 9, message = "商品图片最多 9 张")
    private List<String> imageUrls = new ArrayList<>();

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
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls == null ? new ArrayList<>() : imageUrls; }
}
