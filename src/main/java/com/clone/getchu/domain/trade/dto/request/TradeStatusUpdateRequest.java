package com.clone.getchu.domain.trade.dto.request;

import com.clone.getchu.domain.trade.enums.TradeStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 거래 상태 변경 요청 DTO
 * status 는 TradeStatus enum 값(RESERVED, TRADING, SOLD) 중 하나여야 합니다.
 */
public record TradeStatusUpdateRequest(
        @NotNull(message = "status 값은 필수입니다.")
        TradeStatus status
) {
}

