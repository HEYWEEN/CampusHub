package com.campushub.team.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.response.PageResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.team.dto.TeamApplicationCreateDTO;
import com.campushub.team.dto.TeamApplicationReviewDTO;
import com.campushub.team.dto.TeamRecruitCreateDTO;
import com.campushub.team.dto.TeamRecruitQueryDTO;
import com.campushub.team.service.TeamService;
import com.campushub.team.vo.TeamApplicationVO;
import com.campushub.team.vo.TeamRecruitVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * /api/team/* + /api/search/teams —— 组队招募 / 申请 / 审核（F-TEAM-01~04）。
 */
@RestController
@RequestMapping("/api")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /** 组队大厅（F-TEAM-04）。 */
    @GetMapping("/search/teams")
    public ApiResponse<PageResponse<TeamRecruitVO>> searchTeams(@Valid TeamRecruitQueryDTO query) {
        return ApiResponse.success(teamService.search(query, CurrentUserHolder.getUserId()));
    }

    /** 发布招募（F-TEAM-01，登录即可）。 */
    @PostMapping("/team/recruits")
    public ApiResponse<TeamRecruitVO> create(@Valid @RequestBody TeamRecruitCreateDTO dto) {
        return ApiResponse.success(teamService.createRecruit(CurrentUserHolder.getUserId(), dto));
    }

    @GetMapping("/team/recruits/{id}")
    public ApiResponse<TeamRecruitVO> detail(@PathVariable("id") long recruitId) {
        return ApiResponse.success(teamService.getDetail(recruitId, CurrentUserHolder.getUserId()));
    }

    /** 申请加入（F-TEAM-02）。 */
    @PostMapping("/team/recruits/{id}/applications")
    public ApiResponse<Void> apply(@PathVariable("id") long recruitId,
                                    @Valid @RequestBody(required = false) TeamApplicationCreateDTO dto) {
        teamService.apply(CurrentUserHolder.getUserId(), recruitId,
                dto == null ? new TeamApplicationCreateDTO() : dto);
        return ApiResponse.success();
    }

    /** 队长查看申请列表（F-TEAM-03）。 */
    @GetMapping("/team/recruits/{id}/applications")
    public ApiResponse<List<TeamApplicationVO>> applications(@PathVariable("id") long recruitId) {
        return ApiResponse.success(teamService.listApplications(CurrentUserHolder.getUserId(), recruitId));
    }

    /** 队长审核同意/拒绝（F-TEAM-03）。 */
    @PatchMapping("/team/applications/{id}")
    public ApiResponse<Void> review(@PathVariable("id") long applicationId,
                                     @Valid @RequestBody TeamApplicationReviewDTO dto) {
        teamService.review(CurrentUserHolder.getUserId(), applicationId, dto);
        return ApiResponse.success();
    }
}
