package com.clone.getchu.domain.review.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        BigDecimal rating,
        String content,
        LocalDateTime createdAt,
        String reviewerNickname,
        String reviewerProfileImageUrl,
        String productTitle
) {}
