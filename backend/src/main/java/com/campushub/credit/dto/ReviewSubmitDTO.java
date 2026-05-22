package com.campushub.credit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 提交任务评价请求体（CRD-02，POST /api/credit/reviews）。
 * reviewer 取自当前登录态，不在请求体里传，防伪造。
 */
public class ReviewSubmitDTO {

    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    @NotNull(message = "revieweeId 不能为空")
    private Long revieweeId;

    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private int rating;

    @Size(max = 500, message = "评价文字不超过 500 字")
    private String comment;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getRevieweeId() { return revieweeId; }
    public void setRevieweeId(Long revieweeId) { this.revieweeId = revieweeId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
