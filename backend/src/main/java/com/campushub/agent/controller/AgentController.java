package com.campushub.agent.controller;

import com.campushub.agent.dto.AgentChatRequest;
import com.campushub.agent.service.AgentService;
import com.campushub.agent.vo.AgentChatResponse;
import com.campushub.agent.vo.AgentMessageVO;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/agent/* —— AI 助手（在 /api/** 下，自动需登录）。 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest req) {
        return ApiResponse.success(agentService.chat(CurrentUserHolder.getUserId(), req.getMessage()));
    }

    @GetMapping("/history")
    public ApiResponse<List<AgentMessageVO>> history() {
        return ApiResponse.success(agentService.history(CurrentUserHolder.getUserId()));
    }
}
