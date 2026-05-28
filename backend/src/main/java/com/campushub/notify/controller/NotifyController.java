package com.campushub.notify.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.notify.service.NotifyService;
import com.campushub.notify.vo.NotifyMessageVO;
import com.campushub.notify.vo.UnreadCountVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站内信前台接口（NTF-01，需登录）。路径与前端 src/api/notify.ts 严格对齐：
 * <ul>
 *   <li>GET    /api/notify/messages?filter=all|unread|read       —— 列表（默认 all，未读优先 + 时间倒序）</li>
 *   <li>GET    /api/notify/messages/unread-count                  —— 未读数量</li>
 *   <li>PATCH  /api/notify/messages/{id}/read                     —— 单条已读</li>
 *   <li>POST   /api/notify/messages/read-all                      —— 全部已读</li>
 * </ul>
 *
 * <p>站内信"发"由内部 listener / NotifyApi 触发，无前台 POST。
 */
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    private final NotifyService notifyService;

    public NotifyController(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @GetMapping("/messages")
    public ApiResponse<List<NotifyMessageVO>> listMessages(
            @RequestParam(required = false, defaultValue = "all") String filter) {
        return ApiResponse.success(notifyService.listMine(CurrentUserHolder.getUserId(), filter));
    }

    @GetMapping("/messages/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount() {
        return ApiResponse.success(new UnreadCountVO(notifyService.countUnread(CurrentUserHolder.getUserId())));
    }

    @PatchMapping("/messages/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable("id") long id) {
        notifyService.markRead(CurrentUserHolder.getUserId(), id);
        return ApiResponse.success();
    }

    @PostMapping("/messages/read-all")
    public ApiResponse<Void> markAllRead() {
        notifyService.markAllRead(CurrentUserHolder.getUserId());
        return ApiResponse.success();
    }
}
