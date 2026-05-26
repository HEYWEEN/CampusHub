package com.campushub.task.dto;

import com.campushub.task.entity.TaskType;

import java.time.Instant;

public class TaskQueryDTO {

    private int page = 1;
    private int size = 20;
    private String sort = "createdAt,desc";
    private TaskType taskType;
    private Integer status;
    private Integer minCredit;
    private Instant deadlineFrom;
    private Instant deadlineTo;
    private String q;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = Math.min(Math.max(1, size), 100); }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType v) { this.taskType = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public Integer getMinCredit() { return minCredit; }
    public void setMinCredit(Integer v) { this.minCredit = v; }
    public Instant getDeadlineFrom() { return deadlineFrom; }
    public void setDeadlineFrom(Instant v) { this.deadlineFrom = v; }
    public Instant getDeadlineTo() { return deadlineTo; }
    public void setDeadlineTo(Instant v) { this.deadlineTo = v; }
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
}
