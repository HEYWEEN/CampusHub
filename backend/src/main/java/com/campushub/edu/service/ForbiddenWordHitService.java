package com.campushub.edu.service;

import com.campushub.edu.entity.ForbiddenWordHit;
import com.campushub.edu.repository.ForbiddenWordHitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class ForbiddenWordHitService {

    private static final int COOLDOWN_THRESHOLD = 3;
    private static final Duration COOLDOWN_DURATION = Duration.ofHours(24);

    private final ForbiddenWordHitRepository hitRepo;

    public ForbiddenWordHitService(ForbiddenWordHitRepository hitRepo) {
        this.hitRepo = hitRepo;
    }

  /** 独立事务写入，避免发布失败回滚时丢失命中计数。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHit(long userId) {
        ForbiddenWordHit hit = hitRepo.findById(userId).orElseGet(() -> new ForbiddenWordHit(userId));
        hit.incrementHit();
        if (hit.getHitCount() >= COOLDOWN_THRESHOLD) {
            hit.startCooldown(Instant.now().plus(COOLDOWN_DURATION));
        }
        hitRepo.save(hit);
    }
}
