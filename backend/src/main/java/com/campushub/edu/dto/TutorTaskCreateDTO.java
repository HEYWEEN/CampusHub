package com.campushub.edu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TutorTaskCreateDTO {

    @NotBlank
    @Size(max = 120)
    private String subject;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @Min(1)
    private int rewardPoint;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getRewardPoint() { return rewardPoint; }
    public void setRewardPoint(int rewardPoint) { this.rewardPoint = rewardPoint; }
}
