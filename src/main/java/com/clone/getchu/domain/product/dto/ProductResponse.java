package com.clone.getchu.domain.product.dto;

import com.clone.getchu.domain.product.entity.Product;

public record ProductResponse(
        Long id,
        String title,
        String status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getTitle(), product.getStatus());
    }
}