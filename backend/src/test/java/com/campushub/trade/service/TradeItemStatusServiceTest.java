package com.campushub.trade.service;

import com.campushub.common.exception.BizException;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeItemStatus;
import com.campushub.trade.exception.TradeErrorCode;
import com.campushub.trade.repository.TradeItemImageRepository;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.vo.TradeItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TradeItemStatusServiceTest {

    @Autowired private TradeItemService itemService;
    @Autowired private TradeItemRepository itemRepo;
    @Autowired private TradeItemImageRepository imageRepo;

    @BeforeEach
    void cleanup() {
        imageRepo.deleteAll();
        itemRepo.deleteAll();
    }

    @Test
    void updateStatus_forbiddenWhenNotOwner() {
        TradeItemVO item = itemService.createItem(1L, dto("商品"), List.of());
        TradeItemStatusPatchDTO patch = new TradeItemStatusPatchDTO();
        patch.setStatus(TradeItemStatus.OFF_SALE);

        BizException ex = assertThrows(BizException.class,
                () -> itemService.updateStatus(999L, item.id(), patch));
        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void updateStatus_inTradeCannotChange() {
        TradeItemVO item = itemService.createItem(2L, dto("交易中"), List.of());
        var entity = itemRepo.findById(item.id()).orElseThrow();
        entity.setStatus(TradeItemStatus.IN_TRADE);
        itemRepo.save(entity);

        TradeItemStatusPatchDTO patch = new TradeItemStatusPatchDTO();
        patch.setStatus(TradeItemStatus.OFF_SALE);

        BizException ex = assertThrows(BizException.class,
                () -> itemService.updateStatus(2L, item.id(), patch));
        assertEquals(422, ex.getHttpStatus());
        assertEquals(TradeErrorCode.ITEM_STATUS_INVALID, ex.getCode());
    }

  @Test
    void updateStatus_ownerCanOffSale() {
        TradeItemVO item = itemService.createItem(3L, dto("可下架"), List.of());
        TradeItemStatusPatchDTO patch = new TradeItemStatusPatchDTO();
        patch.setStatus(TradeItemStatus.OFF_SALE);

        TradeItemVO vo = itemService.updateStatus(3L, item.id(), patch);
        assertEquals(TradeItemStatus.OFF_SALE, vo.status());
    }

    private static TradeItemCreateDTO dto(String title) {
        TradeItemCreateDTO dto = new TradeItemCreateDTO();
        dto.setTitle(title);
        dto.setPricePoint(10);
        dto.setPickupLocationType(PickupLocationType.MEETUP);
        return dto;
    }
}
