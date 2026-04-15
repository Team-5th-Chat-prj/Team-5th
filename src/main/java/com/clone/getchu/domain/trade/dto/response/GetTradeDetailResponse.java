package com.clone.getchu.domain.trade.dto.response;

import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeStatus;

import java.time.LocalDateTime;

public record GetTradeDetailResponse(
        String productTitle,
        TradeStatus status,
        Integer price,
        LocalDateTime productCreatedAt, //상품 게시일
        LocalDateTime soldAt, //거래 완료일
        String sellerNickname,
        String buyerNickname
) {
    public static GetTradeDetailResponse from(Trade trade) {
        return new GetTradeDetailResponse(
                trade.getProduct().getTitle(),
                trade.getStatus(),
                trade.getProduct().getPrice(),
                trade.getProduct().getCreatedAt(),
                trade.getSoldAt(),
                trade.getSeller().getNickname(),
                trade.getBuyer().getNickname()
        );
    }
}
