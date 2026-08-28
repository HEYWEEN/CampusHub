package com.campushub.agent.exception;

/** 当前 AI Provider 不可用（无 key / 超时 / 非 2xx / 解析失败）。由 AgentService 捕获并降级。 */
public class AgentUnavailableException extends RuntimeException {
    public AgentUnavailableException(String message) {
        super(message);
    }

    public AgentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
