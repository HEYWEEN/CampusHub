package com.campushub.agent.vo;

import com.campushub.task.vo.TaskListItemVO;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** 助手回复里附带的结构化动作：搜索结果卡片 / 发单草稿。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentAction(
        String type,                 // "task_results" | "task_draft"
        List<TaskListItemVO> tasks,
        TaskDraftVO draft
) {
    public static AgentAction taskResults(List<TaskListItemVO> tasks) {
        return new AgentAction("task_results", tasks, null);
    }

    public static AgentAction taskDraft(TaskDraftVO draft) {
        return new AgentAction("task_draft", null, draft);
    }
}
