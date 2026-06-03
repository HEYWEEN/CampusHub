package com.campushub.agent.vo;

import java.util.List;

/** GET /api/agent/history —— 最近一条会话的 id + 消息（conversationId 为 null 表示还没有会话）。 */
public record AgentHistoryVO(Long conversationId, List<AgentMessageVO> messages) {}
