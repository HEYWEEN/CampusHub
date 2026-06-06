package com.campushub.team.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.response.PageResponse;
import com.campushub.credit.api.CreditApi;
import com.campushub.notify.api.NotifyApi;
import com.campushub.team.dto.TeamApplicationCreateDTO;
import com.campushub.team.dto.TeamApplicationReviewDTO;
import com.campushub.team.dto.TeamRecruitCreateDTO;
import com.campushub.team.dto.TeamRecruitQueryDTO;
import com.campushub.team.entity.TeamApplication;
import com.campushub.team.entity.TeamApplicationStatus;
import com.campushub.team.entity.TeamRecruit;
import com.campushub.team.entity.TeamRecruitStatus;
import com.campushub.team.exception.TeamErrorCode;
import com.campushub.team.repository.TeamApplicationRepository;
import com.campushub.team.repository.TeamRecruitRepository;
import com.campushub.team.vo.TeamApplicationVO;
import com.campushub.team.vo.TeamRecruitVO;
import com.campushub.user.api.UserApi;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {

    private static final int APPLY_MAX_PER_DAY = 3;

    private final TeamRecruitRepository recruitRepo;
    private final TeamApplicationRepository appRepo;
    private final UserApi userApi;
    private final CreditApi creditApi;
    private final NotifyApi notifyApi;

    public TeamServiceImpl(TeamRecruitRepository recruitRepo,
                           TeamApplicationRepository appRepo,
                           UserApi userApi,
                           CreditApi creditApi,
                           NotifyApi notifyApi) {
        this.recruitRepo = recruitRepo;
        this.appRepo = appRepo;
        this.userApi = userApi;
        this.creditApi = creditApi;
        this.notifyApi = notifyApi;
    }

    // ==================== F-TEAM-01 发帖 ====================

    @Override
    @Transactional
    public TeamRecruitVO createRecruit(long userId, TeamRecruitCreateDTO dto) {
        String tags = dto.getSkillTags().stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .limit(5)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        TeamRecruit r = recruitRepo.save(new TeamRecruit(
                userId, dto.getTitle().trim(),
                dto.getDescription() == null ? null : dto.getDescription().trim(),
                tags, dto.getTotalSize()));

        return TeamRecruitVO.from(r, userApi.getPublicUser(userId), true, null);
    }

    // ==================== F-TEAM-04 大厅 ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeamRecruitVO> search(TeamRecruitQueryDTO query, long currentUserId) {
        Specification<TeamRecruit> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.isNull(root.get("deletedAt")));
            if (query.getStatus() != null) {
                preds.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getTag() != null && !query.getTag().isBlank()) {
                preds.add(cb.like(root.get("skillTags"), "%" + query.getTag().trim() + "%"));
            }
            if (query.getQ() != null && !query.getQ().isBlank()) {
                String like = "%" + query.getQ().trim() + "%";
                preds.add(cb.or(cb.like(root.get("title"), like), cb.like(root.get("description"), like)));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(query.getPage() - 1, query.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TeamRecruit> page = recruitRepo.findAll(spec, pageable);
        return PageResponse.of(page, r -> TeamRecruitVO.from(
                r, userApi.getPublicUser(r.getCreatorId()), r.getCreatorId() == currentUserId, null));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamRecruitVO getDetail(long recruitId, long currentUserId) {
        TeamRecruit r = findRecruit(recruitId);
        TeamApplication myApp = appRepo
                .findFirstByRecruitIdAndApplicantIdOrderByCreatedAtDesc(recruitId, currentUserId)
                .orElse(null);
        return TeamRecruitVO.from(r, userApi.getPublicUser(r.getCreatorId()),
                r.getCreatorId() == currentUserId, myApp);
    }

    // ==================== F-TEAM-02 申请 ====================

    @Override
    @Transactional
    public void apply(long userId, long recruitId, TeamApplicationCreateDTO dto) {
        TeamRecruit r = findRecruit(recruitId);

        if (r.getCreatorId() == userId) {
            throw new BizException(TeamErrorCode.SELF_APPLY, "不能申请自己发起的组队");
        }
        if (r.getStatus() != TeamRecruitStatus.RECRUITING) {
            throw new BizException(TeamErrorCode.RECRUIT_CLOSED, "该组队已结束招募", 409);
        }
        if (r.isFull()) {
            throw new BizException(TeamErrorCode.ALREADY_FULL, "队伍已满员", 409);
        }

        Instant since = Instant.now().minus(Duration.ofHours(24));
        long recent = appRepo.countByRecruitIdAndApplicantIdAndCreatedAtAfter(recruitId, userId, since);
        if (recent >= APPLY_MAX_PER_DAY) {
            throw new BizException(TeamErrorCode.APPLY_RATE_LIMIT, "24 小时内对同一帖申请已达上限(3 次)", 429);
        }

        appRepo.save(new TeamApplication(recruitId, userId,
                dto.getMessage() == null ? null : dto.getMessage().trim()));
    }

    // ==================== F-TEAM-03 队长审核 ====================

    @Override
    @Transactional(readOnly = true)
    public List<TeamApplicationVO> listApplications(long captainId, long recruitId) {
        TeamRecruit r = findRecruit(recruitId);
        if (r.getCreatorId() != captainId) {
            throw new BizException(TeamErrorCode.NOT_CAPTAIN, "仅队长可查看申请", 403);
        }
        return appRepo.findByRecruitIdOrderByCreatedAtDesc(recruitId).stream()
                .map(a -> TeamApplicationVO.from(a,
                        userApi.getPublicUser(a.getApplicantId()),
                        creditApi.getScoreOf(a.getApplicantId())))
                .toList();
    }

    @Override
    @Transactional
    public void review(long captainId, long applicationId, TeamApplicationReviewDTO dto) {
        TeamApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("申请不存在: " + applicationId));
        TeamRecruit r = findRecruit(app.getRecruitId());

        if (r.getCreatorId() != captainId) {
            throw new BizException(TeamErrorCode.NOT_CAPTAIN, "仅队长可审核", 403);
        }
        if (app.getStatus() != TeamApplicationStatus.PENDING) {
            throw new BizException(TeamErrorCode.APPLICATION_NOT_PENDING, "该申请已处理", 409);
        }

        String bizKey = "TEAM_APPLY_RESULT:" + app.getId() + ":" + app.getApplicantId();
        if (Boolean.TRUE.equals(dto.getApprove())) {
            if (r.getStatus() != TeamRecruitStatus.RECRUITING || r.isFull()) {
                throw new BizException(TeamErrorCode.ALREADY_FULL, "队伍已满员，无法再通过", 409);
            }
            app.setStatus(TeamApplicationStatus.APPROVED);
            r.joinOne();
            recruitRepo.save(r);
            appRepo.save(app);
            notifyApi.appendLetter(app.getApplicantId(), "TEAM_APPLY_RESULT",
                    "组队申请已通过", "你加入《" + r.getTitle() + "》的申请已通过 🎉", bizKey);
        } else {
            app.setStatus(TeamApplicationStatus.REJECTED);
            appRepo.save(app);
            notifyApi.appendLetter(app.getApplicantId(), "TEAM_APPLY_RESULT",
                    "组队申请被拒绝", "你加入《" + r.getTitle() + "》的申请未通过", bizKey);
        }
    }

    // ==================== 队长关闭招募 ====================

    @Override
    @Transactional
    public void closeRecruit(long captainId, long recruitId) {
        TeamRecruit r = findRecruit(recruitId);
        if (r.getCreatorId() != captainId) {
            throw new BizException(TeamErrorCode.NOT_CAPTAIN, "仅队长可关闭招募", 403);
        }
        if (r.getStatus() == TeamRecruitStatus.CLOSED) {
            throw new BizException(TeamErrorCode.RECRUIT_CLOSED, "该组队已结束招募", 409);
        }
        r.setStatus(TeamRecruitStatus.CLOSED);
        recruitRepo.save(r);
    }

    // ==================== 内部 ====================

    private TeamRecruit findRecruit(long recruitId) {
        TeamRecruit r = recruitRepo.findById(recruitId)
                .orElseThrow(() -> new NotFoundException("组队帖不存在: " + recruitId));
        if (r.getDeletedAt() != null) {
            throw new NotFoundException("组队帖已删除: " + recruitId);
        }
        return r;
    }
}
