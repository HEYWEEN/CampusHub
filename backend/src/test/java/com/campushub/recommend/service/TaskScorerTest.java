package com.campushub.recommend.service;

import com.campushub.recommend.model.UserPreference;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskScorer 纯单测：各维度打分、权重合成、冷启动降权、归一化边界。
 */
class TaskScorerTest {

    private final TaskScorer scorer = new TaskScorer();
    private final Instant now = Instant.now();

    private Task task(TaskType type, int reward, String building, Instant deadline) {
        return new Task(100L, "标题", type, TaskStatus.PENDING_ACCEPT,
                reward, deadline, null, building, null);
    }

    private final UserPreference errandAtZijin = new UserPreference(TaskType.ERRAND, "紫金楼");

    @Test
    void score_alwaysWithinUnitRange() {
        Task t = task(TaskType.ERRAND, 999, "紫金楼", now.plus(1, ChronoUnit.HOURS));
        double s = scorer.score(t, errandAtZijin, 120, 999, now);
        assertTrue(s >= 0 && s <= 1, "score=" + s);
    }

    @Test
    void typeMatch_raisesScore() {
        Instant dl = now.plus(24, ChronoUnit.HOURS);
        Task match = task(TaskType.ERRAND, 50, "其他楼", dl);
        Task miss = task(TaskType.MUTUAL_HELP, 50, "其他楼", dl);

        double sMatch = scorer.score(match, errandAtZijin, 60, 100, now);
        double sMiss = scorer.score(miss, errandAtZijin, 60, 100, now);

        assertEquals(TaskScorer.W_TYPE, sMatch - sMiss, 1e-9); // 仅类型维度不同
    }

    @Test
    void locationMatch_raisesScore() {
        Instant dl = now.plus(24, ChronoUnit.HOURS);
        Task match = task(TaskType.MUTUAL_HELP, 50, "紫金楼", dl);
        Task miss = task(TaskType.MUTUAL_HELP, 50, "仙林楼", dl);

        double diff = scorer.score(match, errandAtZijin, 60, 100, now)
                - scorer.score(miss, errandAtZijin, 60, 100, now);
        assertEquals(TaskScorer.W_LOCATION, diff, 1e-9);
    }

    @Test
    void higherPublisherCredit_scoresHigher() {
        Instant dl = now.plus(24, ChronoUnit.HOURS);
        Task t = task(TaskType.ERRAND, 50, "紫金楼", dl);
        assertTrue(scorer.score(t, errandAtZijin, 120, 100, now)
                > scorer.score(t, errandAtZijin, 0, 100, now));
    }

    @Test
    void higherReward_scoresHigher() {
        Instant dl = now.plus(24, ChronoUnit.HOURS);
        Task hi = task(TaskType.ERRAND, 100, "紫金楼", dl);
        Task lo = task(TaskType.ERRAND, 10, "紫金楼", dl);
        assertTrue(scorer.score(hi, errandAtZijin, 60, 100, now)
                > scorer.score(lo, errandAtZijin, 60, 100, now));
    }

    @Test
    void fresherTask_scoresHigher() {
        Task t = task(TaskType.ERRAND, 50, "紫金楼", now.plus(24, ChronoUnit.HOURS));
        // createdAt ≈ now；用更晚的「现在」模拟任务变旧
        Instant later = t.getCreatedAt().plus(7, ChronoUnit.DAYS);
        assertTrue(scorer.score(t, errandAtZijin, 60, 100, t.getCreatedAt())
                > scorer.score(t, errandAtZijin, 60, 100, later));
    }

    @Test
    void nearerDeadline_scoresHigher() {
        Task urgent = task(TaskType.ERRAND, 50, "紫金楼", now.plus(2, ChronoUnit.HOURS));
        Task relaxed = task(TaskType.ERRAND, 50, "紫金楼", now.plus(40, ChronoUnit.HOURS));
        assertTrue(scorer.score(urgent, errandAtZijin, 60, 100, now)
                > scorer.score(relaxed, errandAtZijin, 60, 100, now));
    }

    @Test
    void expiredDeadline_givesNoUrgency() {
        Task expired = task(TaskType.ERRAND, 50, "紫金楼", now.minus(1, ChronoUnit.HOURS));
        Task future = task(TaskType.ERRAND, 50, "紫金楼", now.plus(2, ChronoUnit.HOURS));
        assertTrue(scorer.score(future, errandAtZijin, 60, 100, now)
                > scorer.score(expired, errandAtZijin, 60, 100, now));
    }

    @Test
    void coldStart_ignoresTypeAndLocation() {
        Instant dl = now.plus(24, ChronoUnit.HOURS);
        Task a = task(TaskType.ERRAND, 50, "紫金楼", dl);
        Task b = task(TaskType.MUTUAL_HELP, 50, "仙林楼", dl);
        // 无偏好 → 类型/位置不参与，二者得分相等
        double sa = scorer.score(a, UserPreference.EMPTY, 60, 100, now);
        double sb = scorer.score(b, UserPreference.EMPTY, 60, 100, now);
        assertEquals(sa, sb, 1e-9);
        assertTrue(sa >= 0 && sa <= 1);
    }

    @Test
    void coldStart_creditAndRewardStillDifferentiate() {
        Instant dl = now.plus(24, ChronoUnit.HOURS);
        Task rich = task(TaskType.ERRAND, 100, "紫金楼", dl);
        Task poor = task(TaskType.ERRAND, 10, "紫金楼", dl);
        assertTrue(scorer.score(rich, UserPreference.EMPTY, 120, 100, now)
                > scorer.score(poor, UserPreference.EMPTY, 0, 100, now));
    }
}
