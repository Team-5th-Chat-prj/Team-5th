package com.clone.getchu.domain.review.dto.request;

import com.clone.getchu.global.validation.ValidRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReviewRequest(
        @NotNull(message = "평점은 필수입니다.")
        @ValidRating
        BigDecimal rating,

        @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(max = 500, message = "리뷰 내용은 500자 이내로 작성해주세요.")
        String content
) {}
