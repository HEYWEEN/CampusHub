package com.campushub.agent.repository;

import com.campushub.agent.entity.AgentConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentConversationRepository extends JpaRepository<AgentConversation, Long> {

    /** 取该用户最近一条会话（v1 单会话模型）。 */
    Optional<AgentConversation> findFirstByUserIdOrderByUpdatedAtDesc(Long userId);
}
