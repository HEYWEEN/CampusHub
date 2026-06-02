package com.campushub.im.repository;

import com.campushub.im.entity.ImMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImMessageRepository extends JpaRepository<ImMessage, Long> {

    Page<ImMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    Optional<ImMessage> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /** 某会话里「不是我发的」且 id 大于我已读到的 id 的消息数 = 我的未读（id 单调，无时间精度问题）。 */
    long countByConversationIdAndSenderIdNotAndIdGreaterThan(Long conversationId, Long myId, Long afterMsgId);
}
