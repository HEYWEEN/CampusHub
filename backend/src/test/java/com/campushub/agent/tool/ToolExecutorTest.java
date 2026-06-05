package com.campushub.agent.tool;

import com.campushub.agent.vo.TaskDraftVO;
import com.campushub.common.PublicUserVO;
import com.campushub.credit.api.CreditApi;
import com.campushub.recommend.service.TaskScorer;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;
import com.campushub.task.repository.TaskRepository;
import com.campushub.team.service.TeamService;
import com.campushub.trade.service.TradeItemService;
import com.campushub.user.api.UserApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToolExecutorTest {

    @Mock TaskRepository taskRepo;
    @Mock UserApi userApi;
    @Mock CreditApi creditApi;
    @Mock TradeItemService tradeItemService;
    @Mock TeamService teamService;

    ToolExecutor executor;
    final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() {
        executor = new ToolExecutor(taskRepo, new TaskScorer(), userApi, creditApi,
                tradeItemService, teamService, json);
        when(userApi.getPublicUser(anyLong()))
                .thenAnswer(i -> new PublicUserVO(i.getArgument(0), "发布者", null, null));
        when(creditApi.getScoreOf(anyLong())).thenReturn(80);
    }

    private Task task(long pub, String title, TaskType type, int reward, String building) {
        return new Task(pub, title, type, TaskStatus.PENDING_ACCEPT, reward,
                Instant.now().plus(24, ChronoUnit.HOURS), null, building, null);
    }

    private void candidates(Task... tasks) {
        when(taskRepo.findTop100ByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(TaskStatus.PENDING_ACCEPT))
                .thenReturn(List.of(tasks));
    }

    @Test
    void searchTasks_keywordHitRanksHigher() {
        candidates(
                task(2L, "帮忙带饭", TaskType.ERRAND, 50, "A"),
                task(3L, "取快递到紫金楼", TaskType.ERRAND, 50, "紫金楼"));

        ObjectNode args = json.createObjectNode();
        ArrayNode kw = args.putArray("keywords");
        kw.add("取快递");

        ToolResult r = executor.execute(ToolSpecs.SEARCH_TASKS, args, 1L);

        assertNotNull(r.action());
        assertEquals("task_results", r.action().type());
        assertEquals("取快递到紫金楼", r.action().tasks().get(0).title()); // 命中关键词排第一
    }

    @Test
    void searchTasks_relaxesWhenTypeFilterEmpty() {
        // 候选里没有 TUTOR，但请求 taskType=TUTOR → 应放宽返回其它而非空
        candidates(task(2L, "带饭", TaskType.ERRAND, 50, "A"));
        ObjectNode args = json.createObjectNode();
        args.put("taskType", "TUTOR");

        ToolResult r = executor.execute(ToolSpecs.SEARCH_TASKS, args, 1L);

        assertFalse(r.action().tasks().isEmpty()); // 放宽后仍有结果
    }

    @Test
    void searchTasks_respectsRewardFilterAndLimit() {
        candidates(
                task(2L, "t1", TaskType.ERRAND, 10, "A"),
                task(3L, "t2", TaskType.ERRAND, 100, "A"));
        ObjectNode args = json.createObjectNode();
        args.put("minReward", 50);

        ToolResult r = executor.execute(ToolSpecs.SEARCH_TASKS, args, 1L);
        assertEquals(1, r.action().tasks().size());
        assertEquals("t2", r.action().tasks().get(0).title());
    }

    @Test
    void draftTask_normalizesFields() {
        ObjectNode args = json.createObjectNode();
        args.put("title", "取快递");
        args.put("taskType", "ERRAND");
        args.put("rewardPoint", 5);
        args.put("deadlineIso", "2026-06-04T18:00:00+08:00");

        ToolResult r = executor.execute(ToolSpecs.DRAFT_TASK, args, 1L);

        assertEquals("task_draft", r.action().type());
        TaskDraftVO d = r.action().draft();
        assertEquals("取快递", d.title());
        assertEquals(TaskType.ERRAND, d.taskType());
        assertEquals(5, d.rewardPoint());
        assertEquals("2026-06-04T18:00:00+08:00", d.deadlineIso());
    }

    @Test
    void draftTask_defaultsTypeWhenMissing() {
        ObjectNode args = json.createObjectNode();
        args.put("title", "随便");
        args.put("rewardPoint", 3);
        args.put("deadlineIso", "2026-06-04T18:00:00+08:00");

        ToolResult r = executor.execute(ToolSpecs.DRAFT_TASK, args, 1L);
        assertEquals(TaskType.ERRAND, r.action().draft().taskType()); // 缺省 ERRAND
    }
}
