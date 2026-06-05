package com.campushub.agent.vo;

import com.campushub.task.vo.TaskListItemVO;
import com.campushub.team.vo.TeamRecruitVO;
import com.campushub.trade.vo.TradeItemVO;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** 助手回复里附带的结构化动作：搜索结果卡片（任务/二手/组队） / 发单草稿。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentAction(
        String type,                 // task_results | task_draft | trade_results | team_results
        List<TaskListItemVO> tasks,
        TaskDraftVO draft,
        List<TradeItemVO> items,
        List<TeamRecruitVO> teams
) {
    public static AgentAction taskResults(List<TaskListItemVO> tasks) {
        return new AgentAction("task_results", tasks, null, null, null);
    }

    public static AgentAction taskDraft(TaskDraftVO draft) {
        return new AgentAction("task_draft", null, draft, null, null);
    }

    public static AgentAction tradeResults(List<TradeItemVO> items) {
        return new AgentAction("trade_results", null, null, items, null);
    }

    public static AgentAction teamResults(List<TeamRecruitVO> teams) {
        return new AgentAction("team_results", null, null, null, teams);
    }
}
