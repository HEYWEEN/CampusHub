package com.campushub.trade.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.response.PageResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemQueryDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.dto.TradeOfferCreateDTO;
import com.campushub.trade.dto.TradeOfferPriceDTO;
import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.service.TradeItemService;
import com.campushub.trade.service.TradeOfferService;
import com.campushub.trade.service.TradeOrderService;
import com.campushub.trade.vo.TradeItemVO;
import com.campushub.trade.vo.TradeOfferVO;
import com.campushub.trade.vo.TradeOrderVO;
import jakarta.validation.Valid;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TradeController {

    private final TradeItemService itemService;
    private final TradeOrderService orderService;
    private final TradeOfferService offerService;

    public TradeController(TradeItemService itemService, TradeOrderService orderService,
                           TradeOfferService offerService) {
        this.itemService = itemService;
        this.orderService = orderService;
        this.offerService = offerService;
    }

    // ================ 二手商品 ================

    /**
     * 二手商品列表（公开浏览，但当前仍走 /api/** 鉴权；如需公开可加白名单）。
     * schema_audit A-1 修复：原本前端调此接口后端不存在。
     */
    @GetMapping("/search/items")
    public ApiResponse<PageResponse<TradeItemVO>> searchItems(@Valid TradeItemQueryDTO query) {
        return ApiResponse.success(itemService.listItems(query));
    }

    /**
     * 二手商品详情。schema_audit A-2 修复：原本前端调此接口后端不存在。
     */
    @GetMapping("/trade/items/{id}")
    public ApiResponse<TradeItemVO> getItem(@PathVariable("id") long itemId) {
        return ApiResponse.success(itemService.getItem(itemId));
    }

    /**
     * 创建二手商品 — 改为 JSON + imageUrls 列表（图片走 POST /api/uploads 预上传）。
     * schema_audit A-3 / A-4 修复：原本是 multipart，与前端 JSON 调用不匹配。
     */
    @PostMapping("/trade/items")
    public ApiResponse<TradeItemVO> createItem(@Valid @RequestBody TradeItemCreateDTO dto) {
        return ApiResponse.success(itemService.createItem(CurrentUserHolder.getUserId(), dto));
    }

    @PatchMapping("/trade/items/{id}/status")
    public ApiResponse<TradeItemVO> updateItemStatus(
            @PathVariable("id") long itemId,
            @Valid @RequestBody TradeItemStatusPatchDTO dto) {
        return ApiResponse.success(itemService.updateStatus(CurrentUserHolder.getUserId(), itemId, dto));
    }

    // ================ 二手订单 ================

    @PostMapping("/trade/orders")
    public ApiResponse<TradeOrderVO> createOrder(@Valid @RequestBody TradeOrderCreateDTO dto) {
        return ApiResponse.success(orderService.createOrder(CurrentUserHolder.getUserId(), dto));
    }

    @GetMapping("/trade/orders/{id}")
    public ApiResponse<TradeOrderVO> getOrder(@PathVariable("id") long orderId) {
        return ApiResponse.success(orderService.getOrder(CurrentUserHolder.getUserId(), orderId));
    }

    @PostMapping("/trade/orders/{id}/confirm")
    public ApiResponse<TradeOrderVO> confirmOrder(@PathVariable("id") long orderId) {
        return ApiResponse.success(orderService.confirmOrder(CurrentUserHolder.getUserId(), orderId));
    }

    @GetMapping("/trade/orders/mine")
    public ApiResponse<List<TradeOrderVO>> myOrders() {
        return ApiResponse.success(orderService.myOrders(CurrentUserHolder.getUserId()));
    }

    @PostMapping("/trade/orders/{id}/cancel")
    public ApiResponse<TradeOrderVO> cancelOrder(@PathVariable("id") long orderId) {
        return ApiResponse.success(orderService.cancelOrder(CurrentUserHolder.getUserId(), orderId));
    }

    // ================ 二手砍价 ================

    @PostMapping("/trade/offers")
    public ApiResponse<TradeOfferVO> createOffer(@Valid @RequestBody TradeOfferCreateDTO dto) {
        return ApiResponse.success(offerService.createOffer(CurrentUserHolder.getUserId(), dto));
    }

    @PostMapping("/trade/offers/{id}/counter")
    public ApiResponse<TradeOfferVO> counterOffer(@PathVariable("id") long offerId,
                                                  @Valid @RequestBody TradeOfferPriceDTO dto) {
        return ApiResponse.success(offerService.counter(CurrentUserHolder.getUserId(), offerId, dto));
    }

    @PostMapping("/trade/offers/{id}/accept")
    public ApiResponse<TradeOfferVO> acceptOffer(@PathVariable("id") long offerId) {
        return ApiResponse.success(offerService.accept(CurrentUserHolder.getUserId(), offerId));
    }

    @PostMapping("/trade/offers/{id}/reject")
    public ApiResponse<TradeOfferVO> rejectOffer(@PathVariable("id") long offerId) {
        return ApiResponse.success(offerService.reject(CurrentUserHolder.getUserId(), offerId));
    }

    @PostMapping("/trade/offers/{id}/cancel")
    public ApiResponse<TradeOfferVO> cancelOffer(@PathVariable("id") long offerId) {
        return ApiResponse.success(offerService.cancel(CurrentUserHolder.getUserId(), offerId));
    }

    @GetMapping("/trade/offers/mine")
    public ApiResponse<List<TradeOfferVO>> myOffers() {
        return ApiResponse.success(offerService.myOffers(CurrentUserHolder.getUserId()));
    }
}
