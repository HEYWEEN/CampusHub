package com.campushub.edu.service;

import com.campushub.common.exception.BizException;
import com.campushub.edu.dto.TutorTaskCreateDTO;
import com.campushub.edu.entity.EduTutorTask;
import com.campushub.edu.exception.EduErrorCode;
import com.campushub.edu.repository.EduTutorTaskRepository;
import com.campushub.edu.repository.ForbiddenWordHitRepository;
import com.campushub.edu.vo.TutorTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class TutorTaskServiceImpl implements TutorTaskService {

    private final EduTutorTaskRepository taskRepo;
    private final ForbiddenWordHitRepository hitRepo;
    private final ForbiddenWordMatcher wordMatcher;
    private final ForbiddenWordHitService hitService;

    public TutorTaskServiceImpl(EduTutorTaskRepository taskRepo,
                                  ForbiddenWordHitRepository hitRepo,
                                  ForbiddenWordMatcher wordMatcher,
                                  ForbiddenWordHitService hitService) {
        this.taskRepo = taskRepo;
        this.hitRepo = hitRepo;
        this.wordMatcher = wordMatcher;
        this.hitService = hitService;
    }

    @Override
    @Transactional
    public TutorTaskVO createTutorTask(long publisherId, TutorTaskCreateDTO dto) {
        ensureNotInCooldown(publisherId);

        String combined = dto.getSubject() + " " + dto.getDescription();
        Optional<String> hit = wordMatcher.match(combined);
        if (hit.isPresent()) {
            hitService.recordHit(publisherId);
            throw new BizException(EduErrorCode.FORBIDDEN_WORD_HIT,
                    "内容包含违禁词：" + hit.get(), 400);
        }

        EduTutorTask task = taskRepo.save(new EduTutorTask(
                publisherId, dto.getSubject(), dto.getDescription(), dto.getRewardPoint()));
        return toVo(task);
    }

    private void ensureNotInCooldown(long userId) {
        hitRepo.findById(userId).ifPresent(hit -> {
            Instant until = hit.getCooldownUntil();
            if (until != null && until.isAfter(Instant.now())) {
                long minutes = Duration.between(Instant.now(), until).toMinutes();
                throw new BizException(EduErrorCode.COOLDOWN_ACTIVE,
                        "发布冷静期中，剩余约 " + Math.max(minutes, 1) + " 分钟", 423);
            }
        });
    }

    static TutorTaskVO toVo(EduTutorTask task) {
        return new TutorTaskVO(
                task.getId(),
                task.getPublisherId(),
                task.getSubject(),
                task.getDescription(),
                task.getRewardPoint(),
                task.getStatus(),
                task.getCreatedAt()
        );
    }
}
