package com.clone.getchu.domain.trade.dto.response;

import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeStatus;

import java.time.LocalDateTime;

/**
 * 거래 상세 조회 응답 DTO
 *
 * - productTitle  : 상품명
 * - status        : 거래 상태 (SALE | RESERVED | TRADING | SOLD)
 * - price         : 가격
 * - productCreatedAt : 상품 게시일
 * - soldAt        : 거래 완료일 (미완료 시 null)
 * - sellerNickname : 판매자명 (product.seller)
 * - buyerNickname  : 구매자명 (trade.member)
 */
public record GetTradeDetailResponse(
        String productTitle,
        TradeStatus status,
        Integer price,
        LocalDateTime productCreatedAt,
        LocalDateTime soldAt,
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
                trade.getProduct().getSeller().getNickname(),
                trade.getMember().getNickname()
        );
    }
}
