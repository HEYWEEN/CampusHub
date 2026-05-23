package com.campushub.trade.service;

import com.campushub.common.exception.BizException;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeOrderStatus;
import com.campushub.trade.event.TradeOrderCompletedEvent;
import com.campushub.trade.exception.TradeErrorCode;
import com.campushub.trade.repository.TradeItemImageRepository;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.repository.TradeOrderRepository;
import com.campushub.trade.vo.TradeItemVO;
import com.campushub.trade.vo.TradeOrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TradeConfirmServiceTest.EventConfig.class)
class TradeConfirmServiceTest {

    @Autowired private TradeItemService itemService;
    @Autowired private TradeOrderService orderService;
    @Autowired private TradeItemRepository itemRepo;
    @Autowired private TradeItemImageRepository imageRepo;
    @Autowired private TradeOrderRepository orderRepo;
    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private CreditRecordRepository recordRepo;
    @Autowired private TradeEventCaptor eventCaptor;

    @BeforeEach
    void cleanup() {
        eventCaptor.clear();
        orderRepo.deleteAll();
        imageRepo.deleteAll();
        itemRepo.deleteAll();
        recordRepo.deleteAll();
        accountRepo.deleteAll();
    }

    @Test
    void confirmOrder_singleSideUpdatesFlags() {
        TradeOrderVO order = createOrder(10L, 20L, 25);
        TradeOrderVO buyerConfirmed = orderService.confirmOrder(20L, order.id());
        assertTrue(buyerConfirmed.buyerConfirmed());
        assertFalse(buyerConfirmed.sellerConfirmed());
        assertEquals(TradeOrderStatus.BUYER_CONFIRMED, buyerConfirmed.status());
    }

    @Test
    void confirmOrder_bothSidesPublishEvent() {
        TradeOrderVO order = createOrder(11L, 21L, 30);
        orderService.confirmOrder(21L, order.id());
        TradeOrderVO completed = orderService.confirmOrder(11L, order.id());

        assertEquals(TradeOrderStatus.COMPLETED, completed.status());
        assertEquals(1, eventCaptor.events.size());
        TradeOrderCompletedEvent event = eventCaptor.events.get(0);
        assertEquals(order.id(), event.orderId());
        assertEquals(21L, event.buyerId());
        assertEquals(11L, event.sellerId());
        assertEquals(30, event.pointAmount());
    }

    @Test
    void confirmOrder_notParticipant_forbidden() {
        TradeOrderVO order = createOrder(12L, 22L, 18);
        BizException ex = assertThrows(BizException.class, () -> orderService.confirmOrder(999L, order.id()));
        assertEquals(403, ex.getHttpStatus());
        assertEquals(TradeErrorCode.ORDER_NOT_PARTICIPANT, ex.getCode());
    }

    private TradeOrderVO createOrder(long sellerId, long buyerId, int price) {
        CreditAccount buyer = new CreditAccount(buyerId);
        buyer.deposit(200);
        accountRepo.save(buyer);

        TradeItemCreateDTO itemDto = new TradeItemCreateDTO();
        itemDto.setTitle("确认测试");
        itemDto.setPricePoint(price);
        itemDto.setPickupLocationType(PickupLocationType.MEETUP);
        TradeItemVO item = itemService.createItem(sellerId, itemDto, List.of());

        TradeOrderCreateDTO orderDto = new TradeOrderCreateDTO();
        orderDto.setItemId(item.id());
        orderDto.setNegotiatedPricePoint(price);
        return orderService.createOrder(buyerId, orderDto);
    }

    static class TradeEventCaptor {
        private final List<TradeOrderCompletedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        void onCompleted(TradeOrderCompletedEvent event) {
            events.add(event);
        }

        void clear() {
            events.clear();
        }
    }

    @TestConfiguration
    static class EventConfig {
        @Bean
        TradeEventCaptor tradeEventCaptor() {
            return new TradeEventCaptor();
        }
    }
}
