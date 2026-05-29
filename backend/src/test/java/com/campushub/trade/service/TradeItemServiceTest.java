package com.campushub.trade.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.util.ExifCleaner;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.entity.PickupLocationType;
import com.campushub.trade.entity.TradeItem;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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

    // 原 createItem_cleansExifFromJpeg 测试已废弃：
    //   schema_audit A-3/A-4 修复后 TradeItemService.createItem 不再接 binary，
    //   只接 imageUrls 列表（由前端先调 /api/uploads 拿 URL）。
    //   EXIF 清洗职责后续应迁移到 ImageStorage 层（TODO）。
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

    private static byte[] buildJpegWithApp1(String payload) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(0xFF); b.write(0xD8);
        writeSegment(b, 0xE1, payload.getBytes(StandardCharsets.ISO_8859_1));
        b.write(0xFF); b.write(0xDA);
        b.write(0x00); b.write(0x02);
        b.write(new byte[]{1, 2, 3});
        b.write(0xFF); b.write(0xD9);
        return b.toByteArray();
    }

    private static void writeSegment(ByteArrayOutputStream b, int marker, byte[] payload) throws Exception {
        b.write(0xFF);
        b.write(marker);
        int segLen = 2 + payload.length;
        b.write((segLen >> 8) & 0xFF);
        b.write(segLen & 0xFF);
        b.write(payload);
    }
}
