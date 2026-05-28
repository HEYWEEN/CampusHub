package com.campushub.notify.repository;

import com.campushub.notify.entity.NotifyMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotifyMessageRepository extends JpaRepository<NotifyMessage, Long> {

    /** 幂等检查：该 bizKey 是否已发过站内信。 */
    boolean existsByBizKey(String bizKey);

    Optional<NotifyMessage> findByBizKey(String bizKey);

    /** 我的全部站内信（未读优先，按时间倒序）。 */
    List<NotifyMessage> findByUserIdOrderByReadAtAscCreatedAtDesc(long userId);

    /** 我的未读站内信。 */
    List<NotifyMessage> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(long userId);

    /** 我的已读站内信。 */
    List<NotifyMessage> findByUserIdAndReadAtIsNotNullOrderByCreatedAtDesc(long userId);

    long countByUserIdAndReadAtIsNull(long userId);
}
