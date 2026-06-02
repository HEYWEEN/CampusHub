package com.campushub.team.dto;

import com.campushub.team.entity.TeamRecruitStatus;

/** GET /api/search/teams — 组队大厅查询（分页参数自带 sanitize）。 */
public class TeamRecruitQueryDTO {

    private int page = 1;
    private int size = 12;
    private String q;
    private String tag;
    /** 状态过滤；为空时默认只看招募中。 */
    private TeamRecruitStatus status;

    public int getPage() { return Math.max(page, 1); }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return Math.min(Math.max(size, 1), 100); }
    public void setSize(int size) { this.size = size; }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public TeamRecruitStatus getStatus() { return status; }
    public void setStatus(TeamRecruitStatus status) { this.status = status; }
}
