package com.campushub.agent.vo;

import com.campushub.agent.entity.AgentMessage;
import com.campushub.agent.entity.AgentRole;

import java.time.Instant;

/** 历史消息视图。 */
public record AgentMessageVO(String role, String content, Instant createdAt) {
    public static AgentMessageVO from(AgentMessage m) {
        return new AgentMessageVO(
                m.getRole() == AgentRole.USER ? "user" : "assistant",
                m.getContent(),
                m.getCreatedAt());
    }
}
