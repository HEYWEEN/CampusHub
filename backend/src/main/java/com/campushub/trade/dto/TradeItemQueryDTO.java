package com.campushub.trade.dto;

import com.campushub.trade.entity.TradeItemStatus;

/**
 * GET /api/search/items 查询参数（query string 绑定）。
 *
 * 当前 MVP：分页 + 关键字 + 状态过滤（默认仅 ON_SALE）；
 * 后续可扩展价格区间、卖家、品类等。
 */
public class TradeItemQueryDTO {

    private int page = 1;
    private int size = 20;
    /** 默认仅列出在售；前端如想看全部可显式传 null（不传该 query 参） */
    private TradeItemStatus status = TradeItemStatus.ON_SALE;
    private String q;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = Math.min(Math.max(1, size), 100); }
    public TradeItemStatus getStatus() { return status; }
    public void setStatus(TradeItemStatus status) { this.status = status; }
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
}
