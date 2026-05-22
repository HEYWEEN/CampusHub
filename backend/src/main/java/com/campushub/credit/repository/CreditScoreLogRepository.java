package com.campushub.credit.repository;

import com.campushub.credit.entity.CreditScoreLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditScoreLogRepository extends JpaRepository<CreditScoreLog, Long> {

    /** 幂等检查：该计分事件是否已记过。 */
    boolean existsByBizId(String bizId);
}
