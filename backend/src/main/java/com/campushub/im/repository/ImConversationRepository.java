package com.campushub.im.repository;

import com.campushub.im.entity.ImConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImConversationRepository extends JpaRepository<ImConversation, Long> {

    /** 按规范化配对（小 id, 大 id）查会话。 */
    Optional<ImConversation> findByUserAIdAndUserBId(Long userAId, Long userBId);

    /** 我参与的所有会话（a 或 b 是我），按最后消息时间倒序。 */
    List<ImConversation> findByUserAIdOrUserBIdOrderByLastMsgAtDesc(Long userAId, Long userBId);
}
