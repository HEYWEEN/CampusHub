package com.campushub.trade.service;

import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeItem;
import com.campushub.trade.entity.TradeItemStatus;
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
class TradeItemServiceTest {

    @Autowired private TradeItemService itemService;
    @Autowired private TradeItemRepository itemRepo;
    @Autowired private TradeItemImageRepository imageRepo;

    @BeforeEach
    void cleanup() {
        imageRepo.deleteAll();
        itemRepo.deleteAll();
    }

    @Test
    void createItem_happyPath() {
        TradeItemCreateDTO dto = dto("二手教材", 50);
        TradeItemVO vo = itemService.createItem(100L, dto);

        assertEquals("二手教材", vo.title());
        assertEquals(50, vo.pricePoint());
        assertEquals(TradeItemStatus.ON_SALE, vo.status());
    }

    @Test
    void createItem_persistsImageUrls() {
        TradeItemCreateDTO dto = dto("带图商品", 30);
        dto.setImageUrls(List.of("/uploads/aa/bb/ccdd.jpg", "/uploads/ee/ff/0011.png"));

        TradeItemVO vo = itemService.createItem(101L, dto);

        TradeItem saved = itemRepo.findAll().get(0);
        assertEquals(2, imageRepo.findByItemIdOrderBySortOrderAsc(saved.getId()).size());
        assertEquals(2, vo.imageUrls().size());
    }

    @Test
    void updateStatus_ownerCanToggle() {
        TradeItemVO created = itemService.createItem(200L, dto("可下架", 10));
        TradeItemStatusPatchDTO patch = new TradeItemStatusPatchDTO();
        patch.setStatus(TradeItemStatus.OFF_SALE);

        TradeItemVO off = itemService.updateStatus(200L, created.id(), patch);
        assertEquals(TradeItemStatus.OFF_SALE, off.status());
    }

    private static TradeItemCreateDTO dto(String title, int price) {
        TradeItemCreateDTO dto = new TradeItemCreateDTO();
        dto.setTitle(title);
        dto.setDescription("描述");
        dto.setPricePoint(price);
        dto.setPickupLocationType(PickupLocationType.BUILDING_RANGE);
        dto.setPickupLocationDetail("3号楼");
        return dto;
    }
}
