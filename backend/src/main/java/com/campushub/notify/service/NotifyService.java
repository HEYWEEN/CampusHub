package com.campushub.notify.service;

import com.campushub.notify.vo.NotifyMessageVO;

import java.util.List;

/**
 * notify 模块对内 service 接口（P3 §3.4）。Controller / Listener 内部用；
 * 跨模块调用走 {@link com.campushub.notify.api.NotifyApi}。
 */
public interface NotifyService {

    /**
     * 我的站内信列表。
     * @param filter "all" / "unread" / "read"；null 等价 "all"
     */
    List<NotifyMessageVO> listMine(long userId, String filter);

    long countUnread(long userId);

    /** 标记单条已读（仅本人可标记自己的消息）。 */
    void markRead(long userId, long messageId);

    /** 全部标记已读。 */
    void markAllRead(long userId);
}
