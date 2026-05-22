package com.campushub.credit.repository;

import com.campushub.credit.entity.CreditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditRecordRepository extends JpaRepository<CreditRecord, Long> {

    /** 幂等检查：该 bizKey 是否已记过流水。 */
    boolean existsByBizId(String bizId);

    /** 我的积分流水，按时间倒序分页。 */
    Page<CreditRecord> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);
}
