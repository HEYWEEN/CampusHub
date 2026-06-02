package com.campushub.im.service;

import com.campushub.common.response.PageResponse;
import com.campushub.im.dto.ImSendMessageDTO;
import com.campushub.im.vo.ImConversationVO;
import com.campushub.im.vo.ImMessageVO;

import java.util.List;

/** 站内私信服务（F-IM-01/02/04）。 */
public interface ImService {

    ImConversationVO getOrCreateConversation(long userId, long peerId);

    List<ImConversationVO> listConversations(long userId);

    /** 拉取会话消息（顺带把当前用户的未读清零）。 */
    PageResponse<ImMessageVO> getMessages(long userId, long conversationId, int page, int size);

    ImMessageVO sendMessage(long userId, long conversationId, ImSendMessageDTO dto);

    long unreadTotal(long userId);

    /** F-IM-01：接单后自动建会话 + 系统消息（由 TaskAcceptedImListener 调用）。 */
    void onTaskAccepted(long publisherId, long accepterId, long taskId);
}
