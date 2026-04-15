package com.clone.getchu.domain.product.dto;

public record ProductSearchCondition(
        String keyword,
        Long categoryId,
        String status, // 명세서 기본값 "SALE"은 서비스 로직에서 처리 권장
        String cursor
) {}
