package com.campushub.im.vo;

import com.campushub.im.entity.ImMessage;
import com.campushub.im.entity.ImMessageType;

import java.time.Instant;

public record ImMessageVO(
        Long messageId,
        Long senderId,
        boolean mine,
        ImMessageType contentType,
        String content,
        Instant createdAt
) {
    public static ImMessageVO from(ImMessage m, long viewerId) {
        return new ImMessageVO(m.getId(), m.getSenderId(), m.getSenderId() == viewerId,
                m.getContentType(), m.getContent(), m.getCreatedAt());
    }
}
