package com.campushub.team.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** POST /api/team/recruits — 发布组队招募（F-TEAM-01）。 */
public class TeamRecruitCreateDTO {

    @NotBlank
    @Size(max = 120, message = "标题最多 120 字")
    private String title;

    @Size(max = 1000, message = "描述最多 1000 字")
    private String description;

    /** 技能标签 1~5 个。 */
    @Size(min = 1, max = 5, message = "技能标签需 1~5 个")
    private List<String> skillTags;

    @Min(value = 2, message = "队伍总人数至少 2 人")
    @Max(value = 50, message = "队伍总人数最多 50 人")
    private int totalSize;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getSkillTags() { return skillTags; }
    public void setSkillTags(List<String> skillTags) { this.skillTags = skillTags; }
    public int getTotalSize() { return totalSize; }
    public void setTotalSize(int totalSize) { this.totalSize = totalSize; }
}
