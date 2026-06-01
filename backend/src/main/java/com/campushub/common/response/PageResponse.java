package com.campushub.common.response;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * 分页出参（P3 §2.3 强制）：{items, total, page, size}
 * page 从 1 开始，size 默认 20、最大 100。
 */
public class PageResponse<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;

    public PageResponse() {}

    public PageResponse(List<T> items, long total, int page, int size) {
        this.items = items == null ? Collections.emptyList() : items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
        return new PageResponse<>(items, total, page, size);
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(Collections.emptyList(), 0L, page, size);
    }

    /**
     * 从 Spring Data {@link Page} 构造：自动把 0 基页号转为 1 基，并按 mapper 转换元素。
     * 收敛各模块"stream().map() + 手填 page/size"的重复样板。
     */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        List<T> items = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(items, page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
