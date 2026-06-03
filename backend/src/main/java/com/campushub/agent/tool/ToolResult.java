package com.campushub.agent.tool;

import com.campushub.agent.vo.AgentAction;

/**
 * 工具执行结果：{@code contentForModel} 回填给 LLM 继续推理；
 * {@code action}（可空）是给前端渲染的结构化卡片。
 */
public record ToolResult(String contentForModel, AgentAction action) {}
