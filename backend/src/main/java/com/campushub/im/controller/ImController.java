package com.campushub.im.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.response.PageResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.im.dto.ImSendMessageDTO;
import com.campushub.im.dto.ImStartConversationDTO;
import com.campushub.im.service.ImService;
import com.campushub.im.vo.ImConversationVO;
import com.campushub.im.vo.ImMessageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** /api/im/* —— 站内私信会话 / 消息 / 未读（F-IM-01/02/04）。 */
@RestController
@RequestMapping("/api/im")
public class ImController {

    private final ImService imService;

    public ImController(ImService imService) {
        this.imService = imService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ImConversationVO>> listConversations() {
        return ApiResponse.success(imService.listConversations(CurrentUserHolder.getUserId()));
    }

    /** 与某人开聊（get-or-create）。 */
    @PostMapping("/conversations")
    public ApiResponse<ImConversationVO> start(@Valid @RequestBody ImStartConversationDTO dto) {
        return ApiResponse.success(
                imService.getOrCreateConversation(CurrentUserHolder.getUserId(), dto.getPeerId()));
    }

    /** 会话消息（倒序分页；拉取即标已读）。 */
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<PageResponse<ImMessageVO>> messages(
            @PathVariable("id") long conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.success(
                imService.getMessages(CurrentUserHolder.getUserId(), conversationId, page, size));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<ImMessageVO> send(@PathVariable("id") long conversationId,
                                          @Valid @RequestBody ImSendMessageDTO dto) {
        return ApiResponse.success(
                imService.sendMessage(CurrentUserHolder.getUserId(), conversationId, dto));
    }

    /** 总未读数（顶栏徽章用，F-IM-04）。 */
    @GetMapping("/unread")
    public ApiResponse<Map<String, Long>> unread() {
        return ApiResponse.success(Map.of("count", imService.unreadTotal(CurrentUserHolder.getUserId())));
    }
}
