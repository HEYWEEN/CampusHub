package com.campushub.trade.service;

import com.campushub.common.exception.BizException;
import com.campushub.credit.api.CreditApi;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeOfferCreateDTO;
import com.campushub.trade.dto.TradeOfferPriceDTO;
import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeItemStatus;
import com.campushub.trade.entity.TradeOfferStatus;
import com.campushub.trade.repository.TradeItemImageRepository;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.repository.TradeOfferRepository;
import com.campushub.trade.repository.TradeOrderRepository;
import com.campushub.trade.vo.TradeItemVO;
import com.campushub.trade.vo.TradeOfferVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TradeOfferServiceTest {

    @Autowired private TradeItemService itemService;
    @Autowired private TradeOfferService offerService;
    @Autowired private TradeOrderRepository orderRepo;
    @Autowired private TradeOfferRepository offerRepo;
    @Autowired private TradeItemRepository itemRepo;
    @Autowired private TradeItemImageRepository imageRepo;
    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private CreditRecordRepository recordRepo;

    static final long SELLER = 700L;
    static final long BUYER = 800L;

    @BeforeEach
    void cleanup() {
        offerRepo.deleteAll();
        orderRepo.deleteAll();
        imageRepo.deleteAll();
        itemRepo.deleteAll();
        recordRepo.deleteAll();
        accountRepo.deleteAll();
    }

    @Test
    void createOffer_happyPath_pendingAwaitingSeller() {
        TradeItemVO item = createItem(SELLER, "自行车", 100);
        TradeOfferVO offer = offerService.createOffer(BUYER, create(item.id(), 60));

        assertEquals(TradeOfferStatus.PENDING, offer.status());
        assertEquals("SELLER", offer.awaitingRole());
        assertEquals(60, offer.pricePoint());
        assertTrue(offer.isBuyer());
        assertFalse(offer.myTurn());   // 买家发起后轮到卖家
    }

    @Test
    void createOffer_ownItem_rejected() {
        TradeItemVO item = createItem(SELLER, "书", 30);
        assertThrows(BizException.class, () -> offerService.createOffer(SELLER, create(item.id(), 20)));
    }

    @Test
    void createOffer_duplicateActive_returns409() {
        TradeItemVO item = createItem(SELLER, "手办", 50);
        offerService.createOffer(BUYER, create(item.id(), 40));
        BizException ex = assertThrows(BizException.class,
                () -> offerService.createOffer(BUYER, create(item.id(), 30)));
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void counter_flipsTurnAndUpdatesPrice() {
        TradeItemVO item = createItem(SELLER, "显示器", 200);
        TradeOfferVO offer = offerService.createOffer(BUYER, create(item.id(), 120));

        TradeOfferVO after = offerService.counter(SELLER, offer.id(), price(160));
        assertEquals(160, after.pricePoint());
        assertEquals("BUYER", after.awaitingRole());   // 卖家还价后轮到买家
    }

    @Test
    void act_wrongTurn_returns409() {
        TradeItemVO item = createItem(SELLER, "键盘", 80);
        TradeOfferVO offer = offerService.createOffer(BUYER, create(item.id(), 50));
        // 现在轮到卖家；买家此时 accept 应 409（不是你的回合）
        BizException ex = assertThrows(BizException.class, () -> offerService.accept(BUYER, offer.id()));
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void accept_bySeller_createsOrderFreezesBuyer() {
        seedBalance(BUYER, 100);
        TradeItemVO item = createItem(SELLER, "课本", 70);
        TradeOfferVO offer = offerService.createOffer(BUYER, create(item.id(), 45));

        TradeOfferVO accepted = offerService.accept(SELLER, offer.id());   // 卖家点同意
        assertEquals(TradeOfferStatus.ACCEPTED, accepted.status());
        assertNotNull(accepted.orderId());
        // 冻的是买家、按成交价 45
        assertTrue(recordRepo.existsByBizId("trade:" + accepted.orderId() + ":freeze"));
        assertEquals(45, orderRepo.findById(accepted.orderId()).orElseThrow().getNegotiatedPricePoint());
        assertEquals(TradeItemStatus.IN_TRADE, itemRepo.findById(item.id()).orElseThrow().getStatus());
    }

    @Test
    void reject_marksRejected() {
        TradeItemVO item = createItem(SELLER, "鼠标", 40);
        TradeOfferVO offer = offerService.createOffer(BUYER, create(item.id(), 25));
        TradeOfferVO rejected = offerService.reject(SELLER, offer.id());
        assertEquals(TradeOfferStatus.REJECTED, rejected.status());
    }

    @Test
    void cancel_byBuyerOk_bySellerForbidden() {
        TradeItemVO item = createItem(SELLER, "台灯", 35);
        TradeOfferVO offer = offerService.createOffer(BUYER, create(item.id(), 20));

        BizException ex = assertThrows(BizException.class, () -> offerService.cancel(SELLER, offer.id()));
        assertEquals(403, ex.getHttpStatus());

        TradeOfferVO canceled = offerService.cancel(BUYER, offer.id());
        assertEquals(TradeOfferStatus.CANCELED, canceled.status());
    }

    // ==================== helpers ====================

    private TradeOfferCreateDTO create(long itemId, int price) {
        TradeOfferCreateDTO dto = new TradeOfferCreateDTO();
        dto.setItemId(itemId);
        dto.setPricePoint(price);
        return dto;
    }

    private TradeOfferPriceDTO price(int p) {
        TradeOfferPriceDTO dto = new TradeOfferPriceDTO();
        dto.setPricePoint(p);
        return dto;
    }

    private void seedBalance(long userId, int balance) {
        CreditAccount acct = new CreditAccount(userId);
        acct.deposit(balance);
        accountRepo.save(acct);
    }

    private TradeItemVO createItem(long sellerId, String title, int price) {
        TradeItemCreateDTO dto = new TradeItemCreateDTO();
        dto.setTitle(title);
        dto.setPricePoint(price);
        dto.setPickupLocationType(PickupLocationType.BUILDING_RANGE);
        return itemService.createItem(sellerId, dto);
    }
}
