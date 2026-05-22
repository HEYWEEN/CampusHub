package com.campushub.credit.vo;

/**
 * 提交评价的响应（CRD-02）。
 *
 * @param reviewId      新建评价 id
 * @param bothReviewed  双方是否都已互评（true 时本次触发双方各 +1 信用分）
 */
public record ReviewResultVO(Long reviewId, boolean bothReviewed) {
}
