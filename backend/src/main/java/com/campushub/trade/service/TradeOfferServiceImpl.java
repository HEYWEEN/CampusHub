package com.campushub.trade.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.response.ResponseCode;
import com.campushub.notify.api.NotifyApi;
import com.campushub.trade.dto.TradeOfferCreateDTO;
import com.campushub.trade.dto.TradeOfferPriceDTO;
import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.entity.AwaitingParty;
import com.campushub.trade.entity.TradeItem;
import com.campushub.trade.entity.TradeItemStatus;
import com.campushub.trade.entity.TradeOffer;
import com.campushub.trade.entity.TradeOfferStatus;
import com.campushub.trade.exception.TradeErrorCode;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.repository.TradeOfferRepository;
import com.campushub.trade.vo.TradeOfferVO;
import com.campushub.trade.vo.TradeOrderVO;
import com.campushub.user.api.UserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 砍价状态机。单行 ping-pong：买家发起 → 双方轮流还价 → 当前回合方 accept 成单。
 *
 * <p><b>同意即成单</b>：accept 直接复用 {@link TradeOrderService#createOrder}（同一事务 join），
 * 买家恒为 offer.buyerId（即便点同意的是卖家）。商品乐观锁/冻结失败 → 整体回滚，offer 不残留 ACCEPTED。
 *
 * <p>积分动作全在 order 侧，本服务不注入 CreditApi。通知直接调 NotifyApi（offer 无第二订阅方，不走事件）。
 */
@Service
public class TradeOfferServiceImpl implements TradeOfferService {

    private final TradeItemRepository itemRepo;
    private final TradeOfferRepository offerRepo;
    private final TradeOrderService orderService;
    private final UserApi userApi;
    private final NotifyApi notifyApi;

    public TradeOfferServiceImpl(TradeItemRepository itemRepo,
                                 TradeOfferRepository offerRepo,
                                 TradeOrderService orderService,
                                 UserApi userApi,
                                 NotifyApi notifyApi) {
        this.itemRepo = itemRepo;
        this.offerRepo = offerRepo;
        this.orderService = orderService;
        this.userApi = userApi;
        this.notifyApi = notifyApi;
    }

    @Override
    @Transactional
    public TradeOfferVO createOffer(long buyerId, TradeOfferCreateDTO dto) {
        TradeItem item = itemRepo.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("商品不存在: " + dto.getItemId()));
        if (item.getSellerId() == buyerId) {
            throw new BizException(ResponseCode.RULE_VIOLATION, "不能对自己的商品出价");
        }
        if (item.getStatus() != TradeItemStatus.ON_SALE) {
            throw new BizException(TradeErrorCode.ITEM_NOT_ON_SALE, "商品不可议价", 409);
        }
        // 应用层防重（MariaDB 无部分唯一索引）：同买家同商品仅一条进行中报价。
        if (offerRepo.existsByItemIdAndBuyerIdAndStatus(item.getId(), buyerId, TradeOfferStatus.PENDING)) {
            throw new BizException(TradeErrorCode.OFFER_ALREADY_ACTIVE, "你对该商品已有进行中的报价", 409);
        }
        // 价 @Min(1) 已校验；允许高于标价（让价/竞价），不 clamp。

        TradeOffer offer = new TradeOffer(item.getId(), buyerId, item.getSellerId(), dto.getPricePoint());
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setAwaitingParty(AwaitingParty.SELLER);
        offer = offerRepo.save(offer);

        notify(item.getSellerId(), "TRADE_OFFER_NEW",
                "收到新报价", "买家对《" + item.getTitle() + "》出价 " + offer.getPricePoint() + " 积分",
                "TRADE_OFFER_NEW:" + offer.getId() + ":" + item.getSellerId());
        return toVo(buyerId, offer);
    }

    @Override
    @Transactional
    public TradeOfferVO counter(long userId, long offerId, TradeOfferPriceDTO dto) {
        TradeOffer offer = requireActingParty(userId, offerId);
        offer.setPricePoint(dto.getPricePoint());
        offer.setAwaitingParty(offer.getAwaitingParty().flip());
        offer = offerRepo.save(offer);

        long recipient = offer.getAwaitingParty() == AwaitingParty.BUYER
                ? offer.getBuyerId() : offer.getSellerId();
        // bizKey 带 v<version> 区分多轮还价，否则二次还价撞键被幂等丢弃。
        notify(recipient, "TRADE_OFFER_COUNTERED",
                "对方还价了", "新价格 " + offer.getPricePoint() + " 积分，轮到你回应",
                "TRADE_OFFER_COUNTERED:" + offer.getId() + ":v" + offer.getVersion() + ":" + recipient);
        return toVo(userId, offer);
    }

    @Override
    @Transactional
    public TradeOfferVO accept(long userId, long offerId) {
        TradeOffer offer = requireActingParty(userId, offerId);

        // 同意即成单：买家恒为 offer.buyerId（卖家点同意也一样）。复用 createOrder（同事务）。
        TradeOrderCreateDTO od = new TradeOrderCreateDTO();
        od.setItemId(offer.getItemId());
        od.setNegotiatedPricePoint(offer.getPricePoint());
        TradeOrderVO order = orderService.createOrder(offer.getBuyerId(), od);

        offer.setStatus(TradeOfferStatus.ACCEPTED);
        offer.setOrderId(order.id());
        offer = offerRepo.save(offer);

        notify(offer.getBuyerId(), "TRADE_OFFER_ACCEPTED",
                "报价达成", "成交价 " + offer.getPricePoint() + " 积分，订单已创建，去「我的交易」确认收货",
                "TRADE_OFFER_ACCEPTED:" + offer.getId() + ":" + offer.getBuyerId());
        notify(offer.getSellerId(), "TRADE_OFFER_ACCEPTED",
                "报价达成", "成交价 " + offer.getPricePoint() + " 积分，订单已创建，去「我的交易」发货确认",
                "TRADE_OFFER_ACCEPTED:" + offer.getId() + ":" + offer.getSellerId());
        return toVo(userId, offer);
    }

    @Override
    @Transactional
    public TradeOfferVO reject(long userId, long offerId) {
        TradeOffer offer = requireActingParty(userId, offerId);
        offer.setStatus(TradeOfferStatus.REJECTED);
        offer = offerRepo.save(offer);

        long other = userId == offer.getBuyerId() ? offer.getSellerId() : offer.getBuyerId();
        notify(other, "TRADE_OFFER_REJECTED",
                "报价被拒绝", "对方拒绝了 " + offer.getPricePoint() + " 积分的报价",
                "TRADE_OFFER_REJECTED:" + offer.getId() + ":" + other);
        return toVo(userId, offer);
    }

    @Override
    @Transactional
    public TradeOfferVO cancel(long userId, long offerId) {
        TradeOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new NotFoundException("报价不存在: " + offerId));
        if (userId != offer.getBuyerId()) {
            throw new BizException(TradeErrorCode.OFFER_NOT_PARTICIPANT, "只有买家可撤回报价", 403);
        }
        if (offer.getStatus() != TradeOfferStatus.PENDING) {
            throw new BizException(TradeErrorCode.OFFER_NOT_PENDING, "报价已结束，无法撤回", 409);
        }
        offer.setStatus(TradeOfferStatus.CANCELED);
        offer = offerRepo.save(offer);   // @Version 防与并发 accept 的竞态

        notify(offer.getSellerId(), "TRADE_OFFER_CANCELED",
                "买家撤回了报价", "对《商品 #" + offer.getItemId() + "》的报价已撤回",
                "TRADE_OFFER_CANCELED:" + offer.getId() + ":" + offer.getSellerId());
        return toVo(userId, offer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeOfferVO> myOffers(long userId) {
        return offerRepo.findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(userId, userId).stream()
                .map(o -> toVo(userId, o))
                .toList();
    }

    // ==================== 内部 ====================

    /** 鉴权核心：仅当前 awaiting 方能 counter/accept/reject。 */
    private TradeOffer requireActingParty(long userId, long offerId) {
        TradeOffer o = offerRepo.findById(offerId)
                .orElseThrow(() -> new NotFoundException("报价不存在: " + offerId));
        if (userId != o.getBuyerId() && userId != o.getSellerId()) {
            throw new BizException(TradeErrorCode.OFFER_NOT_PARTICIPANT, "非报价参与方", 403);
        }
        if (o.getStatus() != TradeOfferStatus.PENDING) {
            throw new BizException(TradeErrorCode.OFFER_NOT_PENDING, "报价已结束", 409);
        }
        AwaitingParty expected = userId == o.getBuyerId() ? AwaitingParty.BUYER : AwaitingParty.SELLER;
        if (o.getAwaitingParty() != expected) {
            throw new BizException(TradeErrorCode.OFFER_NOT_YOUR_TURN, "当前不是你的回合", 409);
        }
        return o;
    }

    private void notify(long userId, String type, String title, String body, String bizKey) {
        notifyApi.appendLetter(userId, type, title, body, bizKey);
    }

    private TradeOfferVO toVo(long viewerId, TradeOffer offer) {
        TradeItem item = itemRepo.findById(offer.getItemId()).orElse(null);
        String title = item != null ? item.getTitle() : "商品 #" + offer.getItemId();
        int itemPrice = item != null ? item.getPricePoint() : offer.getPricePoint();
        PublicUserVO buyer = userApi.findPublicUser(offer.getBuyerId())
                .orElseGet(() -> new PublicUserVO(offer.getBuyerId(), "已注销用户", null, null));
        PublicUserVO seller = userApi.findPublicUser(offer.getSellerId())
                .orElseGet(() -> new PublicUserVO(offer.getSellerId(), "已注销用户", null, null));
        return TradeOfferVO.from(offer, title, itemPrice, buyer, seller, viewerId);
    }
}
