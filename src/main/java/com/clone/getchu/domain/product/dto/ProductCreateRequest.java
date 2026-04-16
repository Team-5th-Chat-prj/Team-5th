package com.clone.getchu.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductCreateRequest(
        @NotBlank(message = "제목은 2자 이상 40자 이하여야 합니다.")
        @Size(min = 2, max = 40)
        String title,

        @NotBlank(message = "설명을 입력해주세요.")
        String description,

        @NotNull(message = "가격을 입력해주세요.")
        @Min(value = 100, message = "가격은 100원 이상이어야 합니다.")
        Integer price,

        @NotNull(message = "카테고리를 선택해주세요.")
        Long categoryId,

        List<String> imageUrls
) {}
