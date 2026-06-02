package com.campushub.im.vo;

import com.campushub.common.PublicUserVO;
import com.campushub.im.entity.ImMessageType;

import java.time.Instant;

public record ImConversationVO(
        Long conversationId,
        PublicUserVO peer,
        String lastMessage,
        ImMessageType lastContentType,
        Instant lastMsgAt,
        long unreadCount
) {}
