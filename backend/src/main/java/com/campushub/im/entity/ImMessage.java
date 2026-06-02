package com.campushub.im.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "im_message",
    indexes = @Index(name = "idx_im_msg_conv_created", columnList = "conversation_id, created_at")
)
public class ImMessage {

    /** 系统消息发送者 id。 */
    public static final long SYSTEM_SENDER = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "content_type", nullable = false)
    private ImMessageType contentType = ImMessageType.TEXT;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ImMessage() {}

    public ImMessage(Long conversationId, Long senderId, ImMessageType contentType, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.contentType = contentType;
        this.content = content;
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public Long getSenderId() { return senderId; }
    public ImMessageType getContentType() { return contentType; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
