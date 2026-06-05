package com.campushub.trade.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.response.ResponseCode;
import com.campushub.credit.api.CreditApi;
import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.entity.TradeItem;
import com.campushub.trade.entity.TradeItemStatus;
import com.campushub.trade.entity.TradeOrder;
import com.campushub.trade.entity.TradeOrderStatus;
import com.campushub.trade.event.TradeOrderCompletedEvent;
import com.campushub.trade.exception.TradeErrorCode;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.repository.TradeOrderRepository;
import com.campushub.trade.vo.TradeOrderVO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeItemRepository itemRepo;
    private final TradeOrderRepository orderRepo;
    private final CreditApi creditApi;
    private final ApplicationEventPublisher eventPublisher;

    public TradeOrderServiceImpl(TradeItemRepository itemRepo,
                                 TradeOrderRepository orderRepo,
                                 CreditApi creditApi,
                                 ApplicationEventPublisher eventPublisher) {
        this.itemRepo = itemRepo;
        this.orderRepo = orderRepo;
        this.creditApi = creditApi;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public TradeOrderVO createOrder(long buyerId, TradeOrderCreateDTO dto) {
        TradeItem item = itemRepo.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("商品不存在: " + dto.getItemId()));

        if (item.getSellerId() == buyerId) {
            throw new BizException(ResponseCode.RULE_VIOLATION, "不能购买自己发布的商品");
        }
        if (item.getStatus() != TradeItemStatus.ON_SALE) {
            throw new BizException(TradeErrorCode.ITEM_NOT_ON_SALE, "商品不可购买", 409);
        }

        int updated = itemRepo.updateStatusIfMatch(
                item.getId(), TradeItemStatus.ON_SALE, TradeItemStatus.IN_TRADE, item.getVersion());
        if (updated == 0) {
            throw new BizException(ResponseCode.CONFLICT, "商品已被其他买家下单");
        }

        TradeOrder order = orderRepo.save(new TradeOrder(
                item.getId(), buyerId, item.getSellerId(), dto.getNegotiatedPricePoint()));

        creditApi.freeze(buyerId, dto.getNegotiatedPricePoint(), "trade:" + order.getId() + ":freeze");

        return toVo(order);
    }

    @Override
    @Transactional(readOnly = true)
    public TradeOrderVO getOrder(long userId, long orderId) {
        TradeOrder order = requireAccessibleOrder(userId, orderId);
        return toVo(order);
    }

    @Override
    @Transactional
    public TradeOrderVO confirmOrder(long userId, long orderId) {
        TradeOrder order = requireAccessibleOrder(userId, orderId);

        if (order.getStatus() == TradeOrderStatus.COMPLETED) {
            throw new BizException(TradeErrorCode.ORDER_ALREADY_CONFIRMED, "订单已完成", 422);
        }
        if (order.getStatus() == TradeOrderStatus.CANCELED) {
            throw new BizException(ResponseCode.RULE_VIOLATION, "订单已取消");
        }

        if (userId == order.getBuyerId()) {
            if (order.isBuyerConfirmed()) {
                return toVo(order);
            }
            order.setBuyerConfirmed(true);
        } else if (userId == order.getSellerId()) {
            if (order.isSellerConfirmed()) {
                return toVo(order);
            }
            order.setSellerConfirmed(true);
        } else {
            throw new BizException(TradeErrorCode.ORDER_NOT_PARTICIPANT, "非订单参与方", 403);
        }

        if (order.isBuyerConfirmed() && order.isSellerConfirmed()) {
            order.setStatus(TradeOrderStatus.COMPLETED);
            eventPublisher.publishEvent(new TradeOrderCompletedEvent(
                    order.getId(),
                    order.getBuyerId(),
                    order.getSellerId(),
                    order.getNegotiatedPricePoint()
            ));
        } else if (order.isBuyerConfirmed()) {
            order.setStatus(TradeOrderStatus.BUYER_CONFIRMED);
        } else if (order.isSellerConfirmed()) {
            order.setStatus(TradeOrderStatus.SELLER_CONFIRMED);
        }

        orderRepo.save(order);
        return toVo(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeOrderVO> myOrders(long userId) {
        return orderRepo.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId).stream()
                .map(TradeOrderServiceImpl::toVo)
                .toList();
    }

    @Override
    @Transactional
    public TradeOrderVO cancelOrder(long userId, long orderId) {
        TradeOrder order = requireAccessibleOrder(userId, orderId);
        if (order.getStatus() == TradeOrderStatus.COMPLETED) {
            throw new BizException(TradeErrorCode.ORDER_ALREADY_CONFIRMED, "订单已完成，无法取消", 422);
        }
        if (order.getStatus() == TradeOrderStatus.CANCELED) {
            return toVo(order);   // 幂等
        }
        order.setStatus(TradeOrderStatus.CANCELED);
        orderRepo.save(order);     // @Version 防与并发 confirm→COMPLETED 的竞态
        // 商品回 ON_SALE（乐观锁，若已被改动则容忍 0 行）
        itemRepo.findById(order.getItemId()).ifPresent(item ->
                itemRepo.updateStatusIfMatch(item.getId(), TradeItemStatus.IN_TRADE,
                        TradeItemStatus.ON_SALE, item.getVersion()));
        // 退还买家押金；bizKey 与完成态 :unfreeze 区分（订单 COMPLETED⊕CANCELED 互斥 → 退款恰一次）
        creditApi.unfreeze(order.getBuyerId(), order.getFreezePoint(), "trade:" + orderId + ":cancel_unfreeze");
        return toVo(order);
    }

    private TradeOrder requireAccessibleOrder(long userId, long orderId) {
        TradeOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在: " + orderId));
        if (userId != order.getBuyerId() && userId != order.getSellerId()) {
            throw new BizException(TradeErrorCode.ORDER_NOT_PARTICIPANT, "非订单参与方", 403);
        }
        return order;
    }

    static TradeOrderVO toVo(TradeOrder order) {
        return new TradeOrderVO(
                order.getId(),
                order.getItemId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getStatus(),
                order.getNegotiatedPricePoint(),
                order.getFreezePoint(),
                order.isBuyerConfirmed(),
                order.isSellerConfirmed(),
                order.getCreatedAt()
        );
    }
}
