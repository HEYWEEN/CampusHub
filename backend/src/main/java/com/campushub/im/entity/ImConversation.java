package com.campushub.im.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "im_conversation",
    uniqueConstraints = @UniqueConstraint(name = "uk_im_conv_pair", columnNames = {"user_a_id", "user_b_id"}),
    indexes = {
        @Index(name = "idx_im_conv_a", columnList = "user_a_id, last_msg_at"),
        @Index(name = "idx_im_conv_b", columnList = "user_b_id, last_msg_at")
    }
)
public class ImConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 参与者较小 id。 */
    @Column(name = "user_a_id", nullable = false)
    private Long userAId;

    /** 参与者较大 id。 */
    @Column(name = "user_b_id", nullable = false)
    private Long userBId;

    @Column(name = "biz_type", length = 16)
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(name = "last_msg_at", nullable = false)
    private Instant lastMsgAt = Instant.now();

    @Column(name = "a_last_read_msg_id")
    private Long aLastReadMsgId;

    @Column(name = "b_last_read_msg_id")
    private Long bLastReadMsgId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ImConversation() {}

    public ImConversation(Long userAId, Long userBId, String bizType, Long bizId) {
        this.userAId = userAId;
        this.userBId = userBId;
        this.bizType = bizType;
        this.bizId = bizId;
    }

    public boolean hasParticipant(long userId) {
        return userAId == userId || userBId == userId;
    }

    /** 返回对方 id。 */
    public long peerOf(long userId) {
        return userAId == userId ? userBId : userAId;
    }

    /** 该用户已读到的最大消息 id（从未读为 0）。 */
    public long lastReadMsgIdOf(long userId) {
        Long v = userId == userAId ? aLastReadMsgId : bLastReadMsgId;
        return v == null ? 0L : v;
    }

    /** 标记已读到 msgId（只前进，不回退）。 */
    public void markReadUpTo(long userId, long msgId) {
        if (userId == userAId) {
            if (aLastReadMsgId == null || msgId > aLastReadMsgId) aLastReadMsgId = msgId;
        } else if (userId == userBId) {
            if (bLastReadMsgId == null || msgId > bLastReadMsgId) bLastReadMsgId = msgId;
        }
        this.updatedAt = Instant.now();
    }

    public void touchLastMsg(Instant at) {
        this.lastMsgAt = at;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserAId() { return userAId; }
    public Long getUserBId() { return userBId; }
    public String getBizType() { return bizType; }
    public Long getBizId() { return bizId; }
    public Instant getLastMsgAt() { return lastMsgAt; }
    public Instant getCreatedAt() { return createdAt; }
}
