package com.campushub.trade.repository;

import com.campushub.trade.entity.TradeItem;
import com.campushub.trade.entity.TradeItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TradeItemRepository extends JpaRepository<TradeItem, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE TradeItem i
               SET i.status = :newStatus, i.version = i.version + 1
             WHERE i.id = :id AND i.status = :expectedStatus AND i.version = :version
            """)
    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("expectedStatus") TradeItemStatus expectedStatus,
                            @Param("newStatus") TradeItemStatus newStatus,
                            @Param("version") int version);

    Optional<TradeItem> findByIdAndSellerId(Long id, Long sellerId);
}
