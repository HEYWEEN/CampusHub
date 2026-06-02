package com.campushub.team.repository;

import com.campushub.team.entity.TeamRecruit;
import com.campushub.team.entity.TeamRecruitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeamRecruitRepository
        extends JpaRepository<TeamRecruit, Long>, JpaSpecificationExecutor<TeamRecruit> {

    long countByCreatorIdAndStatus(Long creatorId, TeamRecruitStatus status);
}
