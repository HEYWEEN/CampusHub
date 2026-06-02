package com.campushub.team.service;

import com.campushub.common.response.PageResponse;
import com.campushub.team.dto.TeamApplicationCreateDTO;
import com.campushub.team.dto.TeamApplicationReviewDTO;
import com.campushub.team.dto.TeamRecruitCreateDTO;
import com.campushub.team.dto.TeamRecruitQueryDTO;
import com.campushub.team.vo.TeamApplicationVO;
import com.campushub.team.vo.TeamRecruitVO;

import java.util.List;

/** 组队模块服务（F-TEAM-01~04）。 */
public interface TeamService {

    TeamRecruitVO createRecruit(long userId, TeamRecruitCreateDTO dto);

    PageResponse<TeamRecruitVO> search(TeamRecruitQueryDTO query, long currentUserId);

    TeamRecruitVO getDetail(long recruitId, long currentUserId);

    void apply(long userId, long recruitId, TeamApplicationCreateDTO dto);

    /** 队长查看某帖的全部申请。 */
    List<TeamApplicationVO> listApplications(long captainId, long recruitId);

    /** 队长审核（同意/拒绝）。 */
    void review(long captainId, long applicationId, TeamApplicationReviewDTO dto);
}
