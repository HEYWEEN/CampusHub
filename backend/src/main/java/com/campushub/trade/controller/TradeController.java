package com.campushub.trade.controller;

import com.campushub.common.exception.BizException;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.exception.TradeErrorCode;
import com.campushub.trade.service.TradeItemService;
import com.campushub.trade.service.TradeOrderService;
import com.campushub.trade.vo.TradeItemVO;
import com.campushub.trade.vo.TradeOrderVO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private static final int MAX_IMAGES = 9;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final TradeItemService itemService;
    private final TradeOrderService orderService;

    public TradeController(TradeItemService itemService, TradeOrderService orderService) {
        this.itemService = itemService;
        this.orderService = orderService;
    }

    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TradeItemVO> createItem(
            @Valid @ModelAttribute TradeItemCreateDTO dto,
            @RequestParam(value = "images", required = false) MultipartFile[] images) throws IOException {
        validateImages(images == null ? List.of() : List.of(images));
        List<TradeItemService.ImageUpload> uploads = toUploads(images);
        return ApiResponse.success(itemService.createItem(CurrentUserHolder.getUserId(), dto, uploads));
    }

    @PatchMapping("/items/{id}/status")
    public ApiResponse<TradeItemVO> updateItemStatus(
            @PathVariable("id") long itemId,
            @Valid @RequestBody TradeItemStatusPatchDTO dto) {
        return ApiResponse.success(itemService.updateStatus(CurrentUserHolder.getUserId(), itemId, dto));
    }

    @PostMapping("/orders")
    public ApiResponse<TradeOrderVO> createOrder(@Valid @RequestBody TradeOrderCreateDTO dto) {
        return ApiResponse.success(orderService.createOrder(CurrentUserHolder.getUserId(), dto));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<TradeOrderVO> getOrder(@PathVariable("id") long orderId) {
        return ApiResponse.success(orderService.getOrder(CurrentUserHolder.getUserId(), orderId));
    }

    @PostMapping("/orders/{id}/confirm")
    public ApiResponse<TradeOrderVO> confirmOrder(@PathVariable("id") long orderId) {
        return ApiResponse.success(orderService.confirmOrder(CurrentUserHolder.getUserId(), orderId));
    }

    private static void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        if (images.size() > MAX_IMAGES) {
            throw new BizException(TradeErrorCode.ITEM_IMAGE_TOO_MANY, "商品图片最多 9 张", 400);
        }
        for (MultipartFile file : images) {
            if (file.getSize() > MAX_IMAGE_BYTES) {
                throw new BizException(TradeErrorCode.IMAGE_SIZE_EXCEEDED, "单张图片不能超过 5MB", 400);
            }
        }
    }

    private static List<TradeItemService.ImageUpload> toUploads(MultipartFile[] images) throws IOException {
        if (images == null || images.length == 0) {
            return List.of();
        }
        List<TradeItemService.ImageUpload> uploads = new ArrayList<>(images.length);
        for (MultipartFile file : images) {
            if (file.isEmpty()) continue;
            uploads.add(new TradeItemService.ImageUpload(file.getBytes(), file.getContentType()));
        }
        return uploads;
    }
}
