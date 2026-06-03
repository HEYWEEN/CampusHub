package com.campushub.agent.vo;

import java.util.List;

/** POST /api/agent/chat 响应。 */
public record AgentChatResponse(
        Long conversationId,
        String reply,
        List<AgentAction> actions
) {}
