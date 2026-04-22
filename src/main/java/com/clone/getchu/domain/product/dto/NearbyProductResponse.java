package com.clone.getchu.domain.product.dto;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;

/**
 * 근처 상품 목록 응답
 * distanceKm: 요청 좌표로부터의 거리 (km, 소수점 1자리)
 */
public record NearbyProductResponse(
        Long id,
        String title,
        Integer price,
        ProductEnum status,
        String categoryName,
        String sellerNickname,
        String thumbnailUrl,    // 첫 번째 이미지 URL (없으면 null)
        String locationName,    // 상품 등록 동네명
        double distanceKm,      // 요청 좌표와의 거리 (km)
        Double lat,
        Double lng
) {
    public static NearbyProductResponse of(Product product, double distanceKm) {
        var location = product.getLocation();
        return new NearbyProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStatus(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getSeller() != null ? product.getSeller().getNickname() : "탈퇴한 사용자",
                product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl(),
                product.getLocationName(),
                distanceKm,
                location != null ? location.getY() : null,
                location != null ? location.getX() : null
        );
    }

    // Object[] 컬럼 순서: id, title, price, status, categoryName, sellerNickname,
    //                     thumbnailUrl, locationName, distanceMeters, lat, lng
    public static NearbyProductResponse from(Object[] row) {
        double distanceMeters = ((Number) row[8]).doubleValue();
        double distanceKm = Math.round(distanceMeters / 1000.0 * 10.0) / 10.0;
        return new NearbyProductResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).intValue(),
                ProductEnum.valueOf((String) row[3]),
                (String) row[4],
                (String) row[5],
                (String) row[6],
                (String) row[7],
                distanceKm,
                row[9] != null ? ((Number) row[9]).doubleValue() : null,
                row[10] != null ? ((Number) row[10]).doubleValue() : null
        );
    }
}
