package com.clone.getchu.domain.trade.enums;

import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;

import java.util.Arrays;

public enum TradeRole {
    SELLER, BUYER;

    public static TradeRole from(String role) {
        return Arrays.stream(values())
                .filter(r -> r.name().equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
    }
}
