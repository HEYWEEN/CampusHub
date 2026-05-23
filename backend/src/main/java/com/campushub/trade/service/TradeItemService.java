package com.campushub.trade.service;

import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.vo.TradeItemVO;

import java.util.List;

public interface TradeItemService {

    record ImageUpload(byte[] bytes, String contentType) {}

    TradeItemVO createItem(long sellerId, TradeItemCreateDTO dto, List<ImageUpload> images);

    TradeItemVO updateStatus(long sellerId, long itemId, TradeItemStatusPatchDTO dto);
}
