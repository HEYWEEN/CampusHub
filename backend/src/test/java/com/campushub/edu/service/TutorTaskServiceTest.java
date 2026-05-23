package com.campushub.edu.service;

import com.campushub.common.exception.BizException;
import com.campushub.edu.dto.TutorTaskCreateDTO;
import com.campushub.edu.entity.ForbiddenWord;
import com.campushub.edu.entity.ForbiddenWordHit;
import com.campushub.edu.exception.EduErrorCode;
import com.campushub.edu.repository.EduTutorTaskRepository;
import com.campushub.edu.repository.ForbiddenWordHitRepository;
import com.campushub.edu.repository.ForbiddenWordRepository;
import com.campushub.edu.vo.TutorTaskVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TutorTaskServiceTest {

    @Autowired private TutorTaskService tutorTaskService;
    @Autowired private EduTutorTaskRepository taskRepo;
    @Autowired private ForbiddenWordRepository wordRepo;
    @Autowired private ForbiddenWordHitRepository hitRepo;

    @BeforeEach
    void cleanup() {
        taskRepo.deleteAll();
        hitRepo.deleteAll();
        wordRepo.deleteAll();
        wordRepo.save(new ForbiddenWord("兼职"));
        wordRepo.save(new ForbiddenWord("代写"));
        wordRepo.save(new ForbiddenWord("刷课"));
    }

    @Test
    void createTutorTask_happyPath() {
        TutorTaskVO vo = tutorTaskService.createTutorTask(1L, dto("高数", "求辅导期末复习", 20));
        assertEquals("高数", vo.subject());
        assertEquals(1, taskRepo.count());
    }

    @Test
    void createTutorTask_forbiddenWord_returns400() {
        BizException ex = assertThrows(BizException.class,
                () -> tutorTaskService.createTutorTask(2L, dto("英语", "需要代写论文", 10)));
        assertEquals(400, ex.getHttpStatus());
        assertEquals(EduErrorCode.FORBIDDEN_WORD_HIT, ex.getCode());
        assertTrue(ex.getMessage().contains("代写"));
    }

    @Test
    void createTutorTask_thirdHitStartsCooldown() {
        for (int i = 0; i < 3; i++) {
            assertThrows(BizException.class,
                    () -> tutorTaskService.createTutorTask(3L, dto("物理", "找刷课渠道", 5)));
        }
        ForbiddenWordHit hit = hitRepo.findById(3L).orElseThrow();
        assertEquals(3, hit.getHitCount());
        assertNotNull(hit.getCooldownUntil());
        assertTrue(hit.getCooldownUntil().isAfter(Instant.now()));
    }

    @Test
    void createTutorTask_cooldownActive_returns423() {
        ForbiddenWordHit hit = new ForbiddenWordHit(4L);
        hit.incrementHit();
        hit.incrementHit();
        hit.incrementHit();
        hit.startCooldown(Instant.now().plusSeconds(3600));
        hitRepo.save(hit);

        BizException ex = assertThrows(BizException.class,
                () -> tutorTaskService.createTutorTask(4L, dto("化学", "正常描述", 8)));
        assertEquals(423, ex.getHttpStatus());
        assertEquals(EduErrorCode.COOLDOWN_ACTIVE, ex.getCode());
    }

    private static TutorTaskCreateDTO dto(String subject, String description, int reward) {
        TutorTaskCreateDTO dto = new TutorTaskCreateDTO();
        dto.setSubject(subject);
        dto.setDescription(description);
        dto.setRewardPoint(reward);
        return dto;
    }
}
