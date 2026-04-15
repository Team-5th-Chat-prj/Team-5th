package com.clone.getchu.domain.trade.dto.response;

import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeStatus;

import java.time.LocalDateTime;

public record GetAllTradeResponse (
        Long tradeId,
        String productTitle,
        Integer price,
        TradeStatus status,
        LocalDateTime updateAt //상태가 마지막으로 변경된 날짜
) {
    public static GetAllTradeResponse from(Trade trade) {
        return new GetAllTradeResponse(
                trade.getId(),
                trade.getProduct().getTitle(),
                trade.getProduct().getPrice(),
                trade.getStatus(),
                trade.getProduct().getUpdatedAt()
        );
    }
}
