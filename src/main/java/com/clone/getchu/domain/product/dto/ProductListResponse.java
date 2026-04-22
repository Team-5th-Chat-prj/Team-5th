package com.clone.getchu.domain.product.dto;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;

import java.time.LocalDateTime;

public record ProductListResponse(
        Long id,
        String title,
        Integer price,
        ProductEnum status,
        String thumbnailUrl,
        Integer likeCount,
        LocalDateTime createdAt
) {
    public static ProductListResponse from(Product product) {
        String thumb = product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl();
        return new ProductListResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStatus(),
                thumb,
                product.getLikeCount(),
                product.getCreatedAt()
        );
    }
}