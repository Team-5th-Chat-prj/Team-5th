package com.clone.getchu.domain.trade.service;

import com.clone.getchu.domain.trade.dto.response.TradeReserveResponse;
import com.clone.getchu.global.lock.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeFacade {

    private final LockService lockService;
    private final TradeService tradeService;

    public TradeReserveResponse reserveProduct(Long productId, Long buyerId) {
        String lockKey = "lock:product:" + productId;
        return lockService.executeWithLock(lockKey, () -> tradeService.reserveProduct(productId, buyerId));
    }
}

