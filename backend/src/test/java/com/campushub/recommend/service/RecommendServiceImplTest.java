package com.campushub.recommend.service;

import com.campushub.common.PublicUserVO;
import com.campushub.credit.api.CreditApi;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;
import com.campushub.task.repository.TaskRepository;
import com.campushub.task.vo.TaskListItemVO;
import com.campushub.user.api.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * RecommendServiceImpl 单测（Mockito + 真实 TaskScorer）：
 * 候选过滤（排除自己发布/自己已接）、偏好聚合、按分降序、截断、冷启动。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommendServiceImplTest {

    @Mock TaskRepository taskRepo;
    @Mock UserApi userApi;
    @Mock CreditApi creditApi;

    RecommendServiceImpl service;

    private static final long ME = 1L;
    private final Instant now = Instant.now();

    @BeforeEach
    void setup() {
        service = new RecommendServiceImpl(taskRepo, new TaskScorer(), userApi, creditApi);
        when(userApi.getPublicUser(anyLong()))
                .thenAnswer(i -> new PublicUserVO(i.getArgument(0), "发布者", null, null));
        when(creditApi.getScoreOf(anyLong())).thenReturn(80);
        // 默认无历史（冷启动），单个用例可覆盖
        when(taskRepo.findTop50ByPublisherIdAndDeletedAtIsNullOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of());
        when(taskRepo.findTop50ByAssigneeIdAndDeletedAtIsNullOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of());
    }

    private Task task(String title, long publisherId, TaskType type, int reward, String building) {
        return new Task(publisherId, title, type, TaskStatus.PENDING_ACCEPT,
                reward, now.plus(24, ChronoUnit.HOURS), null, building, null);
    }

    private void candidates(Task... tasks) {
        when(taskRepo.findTop100ByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(TaskStatus.PENDING_ACCEPT))
                .thenReturn(List.of(tasks));
    }

    @Test
    void emptyCandidates_returnsEmpty() {
        candidates();
        assertTrue(service.recommendTasks(ME, 8).isEmpty());
        verifyNoInteractions(userApi);
    }

    @Test
    void excludesOwnPublishedAndSelfAccepted() {
        Task own = task("我发的", ME, TaskType.ERRAND, 50, "紫金楼");
        Task mineAccepted = task("我接的", 2L, TaskType.ERRAND, 50, "紫金楼");
        mineAccepted.setAssigneeId(ME);
        Task valid = task("可推荐", 3L, TaskType.ERRAND, 50, "紫金楼");
        candidates(own, mineAccepted, valid);

        List<TaskListItemVO> out = service.recommendTasks(ME, 8);

        assertEquals(1, out.size());
        assertEquals("可推荐", out.get(0).title());
    }

    @Test
    void sortsByScoreDescending_coldStart() {
        // 冷启动：高信用+高悬赏应排前。poolMax=100
        Task rich = task("高悬赏", 2L, TaskType.ERRAND, 100, "A");
        Task poor = task("低悬赏", 3L, TaskType.ERRAND, 10, "A");
        candidates(poor, rich); // 故意乱序输入
        when(creditApi.getScoreOf(2L)).thenReturn(120);
        when(creditApi.getScoreOf(3L)).thenReturn(0);

        List<TaskListItemVO> out = service.recommendTasks(ME, 8);

        assertEquals(2, out.size());
        assertEquals("高悬赏", out.get(0).title());
        assertEquals("低悬赏", out.get(1).title());
    }

    @Test
    void respectsLimit() {
        candidates(
                task("t1", 2L, TaskType.ERRAND, 50, "A"),
                task("t2", 3L, TaskType.ERRAND, 50, "A"),
                task("t3", 4L, TaskType.ERRAND, 50, "A"));
        assertEquals(2, service.recommendTasks(ME, 2).size());
    }

    @Test
    void preferenceFromHistory_prioritizesMatchingTypeAndBuilding() {
        // 历史：发过/接过的都是 TUTOR @ "图书馆" → 偏好 TUTOR + 图书馆
        Task hist = task("历史", ME, TaskType.TUTOR, 50, "图书馆");
        when(taskRepo.findTop50ByPublisherIdAndDeletedAtIsNullOrderByCreatedAtDesc(ME))
                .thenReturn(List.of(hist, hist));

        Task matching = task("命中偏好", 2L, TaskType.TUTOR, 50, "图书馆");
        Task other = task("不相关", 3L, TaskType.ERRAND, 50, "别处");
        candidates(other, matching);

        List<TaskListItemVO> out = service.recommendTasks(ME, 8);

        assertEquals("命中偏好", out.get(0).title()); // 类型+位置双命中排第一
    }

    @Test
    void coldStart_noHistory_stillReturnsResults() {
        candidates(task("t1", 2L, TaskType.ERRAND, 50, "A"));
        List<TaskListItemVO> out = service.recommendTasks(ME, 8);
        assertEquals(1, out.size());
    }
}
