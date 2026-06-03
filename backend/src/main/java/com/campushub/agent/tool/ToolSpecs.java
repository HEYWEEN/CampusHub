package com.campushub.agent.tool;

import com.campushub.agent.client.DeepSeekClient.ToolDef;

import java.util.List;
import java.util.Map;

/** 暴露给 DeepSeek 的工具定义（OpenAI function schema）。 */
public final class ToolSpecs {
    private ToolSpecs() {}

    public static final String SEARCH_TASKS = "search_tasks";
    public static final String DRAFT_TASK = "draft_task";

    private static final List<String> TASK_TYPES = List.of("ERRAND", "MUTUAL_HELP", "TUTOR");

    public static List<ToolDef> all() {
        return List.of(searchTasks(), draftTask());
    }

    private static ToolDef searchTasks() {
        return ToolDef.fn(SEARCH_TASKS,
                "按条件检索校园「待接单」任务。用户想找单/接单时调用。尽量把口语映射成结构化字段，"
                        + "并在 keywords 里给出同义词扩展（如「取快递」→[\"取快递\",\"代取\",\"快递\",\"菜鸟驿站\"]）。",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "taskType", Map.of("type", "string", "enum", TASK_TYPES,
                                        "description", "任务类型：ERRAND 跑腿 / MUTUAL_HELP 互助 / TUTOR 辅导"),
                                "keywords", Map.of("type", "array", "items", Map.of("type", "string"),
                                        "description", "关键词及同义词，用于相关度排序"),
                                "minReward", Map.of("type", "integer", "description", "最低悬赏积分"),
                                "maxReward", Map.of("type", "integer", "description", "最高悬赏积分"),
                                "building", Map.of("type", "string", "description", "送达/相关楼栋"),
                                "limit", Map.of("type", "integer", "description", "返回条数，默认 6")
                        ),
                        "required", List.of()
                ));
    }

    private static ToolDef draftTask() {
        return ToolDef.fn(DRAFT_TASK,
                "把用户的发单意图整理成任务草稿（不会直接发布，用户需在发布页确认）。"
                        + "deadlineIso 必须依据 system 提示中的「当前时间」把口语时间换算成 ISO-8601。",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "title", Map.of("type", "string", "description", "简洁任务标题"),
                                "taskType", Map.of("type", "string", "enum", TASK_TYPES),
                                "rewardPoint", Map.of("type", "integer", "description", "悬赏积分"),
                                "deadlineIso", Map.of("type", "string", "description", "ISO-8601 截止时间，如 2026-06-04T18:00:00+08:00"),
                                "deliveryBuilding", Map.of("type", "string", "description", "送达楼栋（可选）"),
                                "remark", Map.of("type", "string", "description", "补充说明（可选）")
                        ),
                        "required", List.of("title", "taskType", "rewardPoint", "deadlineIso")
                ));
    }
}
