package com.campushub.agent.repository;

import com.campushub.agent.entity.AgentMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {

    /** 会话全部消息，按时间正序（渲染历史）。 */
    List<AgentMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** 最近 N 条（喂给模型的上下文），按时间倒序取后再翻转。 */
    List<AgentMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
}
