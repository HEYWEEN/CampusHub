package com.campushub.trade.repository;

import com.campushub.trade.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByIdAndBuyerId(Long id, Long buyerId);

    Optional<TradeOrder> findByIdAndSellerId(Long id, Long sellerId);
}
