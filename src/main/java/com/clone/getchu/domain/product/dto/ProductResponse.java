package com.clone.getchu.domain.product.dto;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.entity.ProductImage;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        Long sellerId,
        String title,
        String description,
        Integer price,
        ProductEnum status,
        Integer likeCount,
        String categoryName,     // 카테고리 ID 대신 이름을 반환
        String sellerNickname,   // 판매자 정보
        List<String> imageUrls,  // 상품 이미지 URL 목록
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSeller().getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                product.getLikeCount(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getSeller() != null ? product.getSeller().getNickname() : "탈퇴한 사용자",
                product.getImages().stream()
                        .map(ProductImage::getImageUrl) // 이미지 엔티티에서 URL만 추출
                        .toList(),
                product.getCreatedAt()
        );
    }
}