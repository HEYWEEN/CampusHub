package com.campushub.trade.repository;

import com.campushub.trade.entity.TradeItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeItemImageRepository extends JpaRepository<TradeItemImage, Long> {

    List<TradeItemImage> findByItemIdOrderBySortOrderAsc(Long itemId);
}
