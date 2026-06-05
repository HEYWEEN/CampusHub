package com.campushub.task.dto;

import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;

import java.time.Instant;

public class TaskQueryDTO {

    private int page = 1;
    private int size = 20;
    private String sort = "createdAt,desc";
    private TaskType taskType;
    // status 接收 enum 字符串（如 PENDING_ACCEPT），Spring 自动按 Enum.valueOf 绑定
    // 原本是 Integer，但前端永远传字符串 → 400（schema_audit A-14 修复）
    private TaskStatus status;
    private Integer minCredit;
    private Instant deadlineFrom;
    private Instant deadlineTo;
    private String q;
    // 「我的任务」过滤：publisherId=我发布的；assigneeId=我接的
    private Long publisherId;
    private Long assigneeId;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = Math.min(Math.max(1, size), 100); }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType v) { this.taskType = v; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus v) { this.status = v; }
    public Integer getMinCredit() { return minCredit; }
    public void setMinCredit(Integer v) { this.minCredit = v; }
    public Instant getDeadlineFrom() { return deadlineFrom; }
    public void setDeadlineFrom(Instant v) { this.deadlineFrom = v; }
    public Instant getDeadlineTo() { return deadlineTo; }
    public void setDeadlineTo(Instant v) { this.deadlineTo = v; }
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long v) { this.publisherId = v; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long v) { this.assigneeId = v; }
}
