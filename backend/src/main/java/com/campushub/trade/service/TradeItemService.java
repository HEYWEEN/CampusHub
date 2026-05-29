package com.campushub.trade.service;

import com.campushub.common.response.PageResponse;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.dto.TradeItemQueryDTO;
import com.campushub.trade.vo.TradeItemVO;

public interface TradeItemService {

    /**
     * 创建商品。
     * 图片由前端预先上传到 /api/uploads 拿到 url 列表，直接在 dto.imageUrls 里。
     * （schema_audit A-3/A-4 修复后改成 JSON 调用，不再处理 binary）
     */
    TradeItemVO createItem(long sellerId, TradeItemCreateDTO dto);

    TradeItemVO updateStatus(long sellerId, long itemId, TradeItemStatusPatchDTO dto);

    /** 列表（分页 + 简单过滤）— 对前端 GET /api/search/items */
    PageResponse<TradeItemVO> listItems(TradeItemQueryDTO query);

    /** 详情 — 对前端 GET /api/trade/items/{id} */
    TradeItemVO getItem(long itemId);
}
