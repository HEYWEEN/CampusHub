package com.campushub.agent.tool;

import com.campushub.agent.vo.AgentAction;
import com.campushub.agent.vo.TaskDraftVO;
import com.campushub.credit.api.CreditApi;
import com.campushub.recommend.model.UserPreference;
import com.campushub.recommend.service.TaskScorer;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;
import com.campushub.task.repository.TaskRepository;
import com.campushub.task.vo.TaskListItemVO;
import com.campushub.user.api.UserApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行器：把 LLM 的工具调用落到真实业务。
 * <ul>
 *   <li>search_tasks：复用候选查询 + {@link TaskScorer} 排序 + 关键词命中加权（不死匹配）</li>
 *   <li>draft_task：仅归一化成草稿，<b>不落库</b></li>
 * </ul>
 */
@Component
public class ToolExecutor {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 12;
    private static final double KEYWORD_WEIGHT = 2.0;

    private final TaskRepository taskRepo;
    private final TaskScorer scorer;
    private final UserApi userApi;
    private final CreditApi creditApi;
    private final ObjectMapper json;

    public ToolExecutor(TaskRepository taskRepo, TaskScorer scorer, UserApi userApi,
                        CreditApi creditApi, ObjectMapper json) {
        this.taskRepo = taskRepo;
        this.scorer = scorer;
        this.userApi = userApi;
        this.creditApi = creditApi;
        this.json = json;
    }

    public ToolResult execute(String toolName, JsonNode args) {
        return switch (toolName) {
            case ToolSpecs.SEARCH_TASKS -> searchTasks(args);
            case ToolSpecs.DRAFT_TASK -> draftTask(args);
            default -> new ToolResult("未知工具: " + toolName, null);
        };
    }

    // ==================== search_tasks ====================

    private ToolResult searchTasks(JsonNode args) {
        TaskType type = parseType(text(args, "taskType"));
        List<String> keywords = stringArray(args.path("keywords"));
        Integer minReward = intOrNull(args, "minReward");
        Integer maxReward = intOrNull(args, "maxReward");
        String building = text(args, "building");
        int limit = args.hasNonNull("limit")
                ? Math.min(Math.max(args.get("limit").asInt(), 1), MAX_LIMIT) : DEFAULT_LIMIT;

        List<Task> candidates = taskRepo
                .findTop100ByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(TaskStatus.PENDING_ACCEPT);

        List<Task> filtered = candidates.stream()
                .filter(t -> type == null || t.getTaskType() == type)
                .filter(t -> minReward == null || t.getRewardPoint() >= minReward)
                .filter(t -> maxReward == null || t.getRewardPoint() <= maxReward)
                .filter(t -> building == null || (t.getDeliveryBuilding() != null
                        && t.getDeliveryBuilding().contains(building)))
                .toList();
        // 稀疏放宽：先只保留类型，再彻底放开
        if (filtered.isEmpty()) {
            filtered = candidates.stream().filter(t -> type == null || t.getTaskType() == type).toList();
        }
        if (filtered.isEmpty()) filtered = candidates;

        Instant now = Instant.now();
        double poolMax = filtered.stream().mapToInt(Task::getRewardPoint).max().orElse(1);
        Map<Long, Integer> creditCache = new HashMap<>();

        List<Task> ranked = filtered.stream()
                .sorted(Comparator.comparingDouble(
                        (Task t) -> relevance(t, keywords, creditCache, poolMax, now)).reversed())
                .limit(limit)
                .toList();

        List<TaskListItemVO> vos = ranked.stream()
                .map(t -> TaskListItemVO.from(t, userApi.getPublicUser(t.getPublisherId())))
                .toList();

        return new ToolResult(modelSummary(vos), AgentAction.taskResults(vos));
    }

    private double relevance(Task t, List<String> keywords, Map<Long, Integer> creditCache,
                             double poolMax, Instant now) {
        int hits = 0;
        if (keywords != null && !keywords.isEmpty()) {
            String hay = (t.getTitle() + " " + nz(t.getRemark()) + " " + nz(t.getDeliveryBuilding()))
                    .toLowerCase();
            for (String k : keywords) {
                if (k != null && !k.isBlank() && hay.contains(k.toLowerCase().trim())) hits++;
            }
        }
        int credit = creditCache.computeIfAbsent(t.getPublisherId(), pid -> creditApi.getScoreOf(pid));
        return hits * KEYWORD_WEIGHT + scorer.score(t, UserPreference.EMPTY, credit, poolMax, now);
    }

    /** 给模型看的精简结果（不含脱敏信息外的多余字段）。 */
    private String modelSummary(List<TaskListItemVO> vos) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TaskListItemVO v : vos) {
            Map<String, Object> m = new HashMap<>();
            m.put("taskId", v.taskId());
            m.put("title", v.title());
            m.put("taskType", v.taskType());
            m.put("reward", v.rewardPoint());
            m.put("building", v.deliveryBuilding());
            m.put("deadline", v.deadlineAt());
            rows.add(m);
        }
        try {
            return "找到 " + vos.size() + " 个任务：" + json.writeValueAsString(rows);
        } catch (Exception e) {
            return "找到 " + vos.size() + " 个任务";
        }
    }

    // ==================== draft_task ====================

    private ToolResult draftTask(JsonNode args) {
        TaskType type = parseType(text(args, "taskType"));
        if (type == null) type = TaskType.ERRAND;
        TaskDraftVO draft = new TaskDraftVO(
                text(args, "title"),
                type,
                args.hasNonNull("rewardPoint") ? args.get("rewardPoint").asInt() : 0,
                text(args, "deadlineIso"),
                text(args, "deliveryBuilding"),
                text(args, "remark"));
        return new ToolResult("已生成发单草稿，等待用户在发布页确认。", AgentAction.taskDraft(draft));
    }

    // ==================== helpers ====================

    private static TaskType parseType(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return TaskType.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static Integer intOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asInt() : null;
    }

    private static List<String> stringArray(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
