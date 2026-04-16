package com.clone.getchu.domain.product.dto;

import com.clone.getchu.domain.product.entity.ProductEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductUpdateRequest(
        @Size(min = 2, max = 40)
        String title,
        String description,
        @Min(100)
        Integer price,
        Long categoryId,
        ProductEnum status,
        List<String> imageUrls // 미전달 시 유지, 전달 시 전체 교체
) {}