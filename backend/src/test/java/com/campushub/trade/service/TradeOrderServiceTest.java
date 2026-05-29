package com.campushub.trade.service;

import com.campushub.common.exception.BizException;
import com.campushub.credit.api.CreditApi;
import com.campushub.credit.entity.CreditAccount;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeOrderCreateDTO;
import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeItemStatus;
import com.campushub.trade.repository.TradeItemImageRepository;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.repository.TradeOrderRepository;
import com.campushub.trade.vo.TradeItemVO;
import com.campushub.trade.vo.TradeOrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TradeOrderServiceTest {

    @Autowired private TradeItemService itemService;
    @Autowired private TradeOrderService orderService;
    @Autowired private TradeItemRepository itemRepo;
    @Autowired private TradeItemImageRepository imageRepo;
    @Autowired private TradeOrderRepository orderRepo;
    @Autowired private CreditApi creditApi;
    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private CreditRecordRepository recordRepo;

    @BeforeEach
    void cleanup() {
        orderRepo.deleteAll();
        imageRepo.deleteAll();
        itemRepo.deleteAll();
        recordRepo.deleteAll();
        accountRepo.deleteAll();
    }

    @Test
    void createOrder_happyPath_freezesBuyerPoints() {
        seedBalance(200L, 100);
        TradeItemVO item = createItem(100L, "教材", 40);

        TradeOrderCreateDTO dto = new TradeOrderCreateDTO();
        dto.setItemId(item.id());
        dto.setNegotiatedPricePoint(40);

        TradeOrderVO order = orderService.createOrder(200L, dto);
        assertEquals(40, order.negotiatedPricePoint());
        assertTrue(recordRepo.existsByBizId("trade:" + order.id() + ":freeze"));

        var savedItem = itemRepo.findById(item.id()).orElseThrow();
        assertEquals(TradeItemStatus.IN_TRADE, savedItem.getStatus());
    }

    @Test
    void createOrder_itemAlreadySold_returns409() {
        seedBalance(201L, 100);
        seedBalance(202L, 100);
        TradeItemVO item = createItem(101L, "唯一商品", 20);

        TradeOrderCreateDTO dto = new TradeOrderCreateDTO();
        dto.setItemId(item.id());
        dto.setNegotiatedPricePoint(20);
        orderService.createOrder(201L, dto);

        BizException ex = assertThrows(BizException.class, () -> orderService.createOrder(202L, dto));
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void createOrder_concurrentOnlyOneSucceeds() throws Exception {
        seedBalance(301L, 100);
        seedBalance(302L, 100);
        TradeItemVO item = createItem(102L, "并发商品", 15);

        TradeOrderCreateDTO dto = new TradeOrderCreateDTO();
        dto.setItemId(item.id());
        dto.setNegotiatedPricePoint(15);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (long buyer : List.of(301L, 302L)) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    orderService.createOrder(buyer, dto);
                    return null;
                } catch (BizException e) {
                    return e;
                }
            }));
        }

        ready.await();
        start.countDown();

        int success = 0;
        int conflict = 0;
        for (Future<?> f : futures) {
            Object result = f.get();
            if (result == null) success++;
            else conflict++;
        }
        pool.shutdown();

        assertEquals(1, success);
        assertEquals(1, conflict);
        assertEquals(1, orderRepo.count());
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
