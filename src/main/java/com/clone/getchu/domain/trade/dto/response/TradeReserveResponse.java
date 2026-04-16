package com.clone.getchu.domain.trade.dto.response;

import com.clone.getchu.domain.trade.entity.Trade;

public record TradeReserveResponse(
        Long tradeId,
        String productTitle,
        String sellerNickname, //판매자 닉네임 (추가됨)
        String buyerNickname   //구매자 닉네임 (본인)
) {
    public static TradeReserveResponse from(Trade trade) {
        return new TradeReserveResponse(
                trade.getId(),
                trade.getProduct().getTitle(),
                trade.getSeller().getNickname(),
                trade.getBuyer().getNickname()
        );
    }
}
