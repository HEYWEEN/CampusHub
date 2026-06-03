package com.campushub.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/agent/chat 请求体。 */
public class AgentChatRequest {

    @NotBlank
    @Size(max = 1000, message = "消息最多 1000 字")
    private String message;

    /** 续聊的会话 id；为空表示开启新对话。 */
    private Long conversationId;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
}
