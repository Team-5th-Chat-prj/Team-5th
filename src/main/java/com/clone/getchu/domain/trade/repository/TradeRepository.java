package com.clone.getchu.domain.trade.repository;

import com.clone.getchu.domain.trade.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
