package com.campushub.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/agent/chat 请求体。 */
public class AgentChatRequest {

    @NotBlank
    @Size(max = 1000, message = "消息最多 1000 字")
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
