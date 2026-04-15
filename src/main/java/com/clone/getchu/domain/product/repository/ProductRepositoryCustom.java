package com.clone.getchu.domain.product.repository;

import com.clone.getchu.domain.product.dto.ProductSearchCondition;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.global.common.CursorPageResponse;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {
    CursorPageResponse<Product> searchByCursor(ProductSearchCondition condition, Pageable pageable);
}