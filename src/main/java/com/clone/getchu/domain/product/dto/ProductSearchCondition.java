package com.clone.getchu.domain.product.dto;

import com.clone.getchu.domain.product.entity.ProductEnum;

public record ProductSearchCondition(
        String keyword,
        Long categoryId,
        ProductEnum status, // 명세서 기본값 "SALE"은 서비스 로직에서 처리 권장
        String cursor
) {}
