package com.campushub.credit.repository;

import com.campushub.credit.entity.CreditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditRecordRepository extends JpaRepository<CreditRecord, Long> {

    /** 幂等检查：该 bizKey 是否已记过流水。 */
    boolean existsByBizId(String bizId);
}
