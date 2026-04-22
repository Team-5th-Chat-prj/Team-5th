package com.clone.getchu.domain.product.repository;

import com.clone.getchu.domain.product.dto.NearbyProductRow;
import com.clone.getchu.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findDetailById(@Param("id") Long id);

    /**
     * 반경 내 상품을 거리 오름차순으로 조회 — Interface Projection으로 타입 안전 매핑
     * 파생 테이블로 ST_Distance_Sphere를 1회만 계산하여 성능 최적화
     */
    @Query(
            value = "SELECT t.id, t.title, t.price, t.status, " +
                    "t.categoryName, t.sellerNickname, t.thumbnailUrl, t.locationName, " +
                    "t.distanceMeters, t.lat, t.lng " +
                    "FROM ( " +
                    "  SELECT p.id AS id, p.title AS title, p.price AS price, p.status AS status, " +
                    "         c.name AS categoryName, m.nickname AS sellerNickname, " +
                    "         (SELECT pi.image_url FROM product_image pi WHERE pi.product_id = p.id LIMIT 1) AS thumbnailUrl, " +
                    "         p.location_name AS locationName, " +
                    "         ST_Distance_Sphere(p.location, ST_SRID(ST_GeomFromText(CONCAT('POINT(', :lng, ' ', :lat, ')')), 4326)) AS distanceMeters, " +
                    "         ST_Y(p.location) AS lat, ST_X(p.location) AS lng " +
                    "  FROM product p " +
                    "  JOIN category c ON p.category_id = c.id " +
                    "  JOIN members m ON p.seller_id = m.id " +
                    "  WHERE p.location IS NOT NULL AND p.is_deleted = false " +
                    "  AND p.status IN ('SALE', 'RESERVED') AND m.deleted = false " +
                    ") AS t " +
                    "WHERE t.distanceMeters <= :radiusMeters " +
                    "ORDER BY t.distanceMeters",
            countQuery = "SELECT COUNT(*) FROM ( " +
                    "  SELECT ST_Distance_Sphere(p.location, ST_SRID(ST_GeomFromText(CONCAT('POINT(', :lng, ' ', :lat, ')')), 4326)) AS distanceMeters " +
                    "  FROM product p " +
                    "  JOIN members m ON p.seller_id = m.id " +
                    "  WHERE p.location IS NOT NULL AND p.is_deleted = false " +
                    "  AND p.status IN ('SALE', 'RESERVED') AND m.deleted = false " +
                    ") AS t WHERE t.distanceMeters <= :radiusMeters",
            nativeQuery = true
    )
    Page<NearbyProductRow> findNearbyProducts(
            @Param("lng") double lng,
            @Param("lat") double lat,
            @Param("radiusMeters") double radiusMeters,
            Pageable pageable);
}

