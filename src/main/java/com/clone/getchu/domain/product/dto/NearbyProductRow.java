package com.clone.getchu.domain.product.dto;

/**
 * 근처 상품 native query 결과 매핑 Interface Projection
 * Object[] 인덱스 접근 대신 이름 기반 접근으로 필드 순서 변경에 안전
 */
public interface NearbyProductRow {
    Long getId();
    String getTitle();
    Integer getPrice();
    String getStatus();
    String getCategoryName();
    String getSellerNickname();
    String getThumbnailUrl();
    String getLocationName();
    Double getDistanceMeters();
    Double getLat();
    Double getLng();
}
