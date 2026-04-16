package com.clone.getchu.domain.product.dto;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;

public record ProductResponse(
        Long id,
        String title,
        ProductEnum status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getTitle(), product.getStatus());
    }
}