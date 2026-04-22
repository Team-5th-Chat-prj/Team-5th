package com.clone.getchu.domain.product.repository;

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
     * 반경 내 상품을 거리 오름차순으로 조회 — DTO 필드를 한 번에 SELECT
     * 단일 쿼리로 처리하여 순서 보장 및 N+1 제거
     * Object[] 컬럼 순서: id, title, price, status, categoryName, sellerNickname,
     *                     thumbnailUrl, locationName, distanceMeters, lat, lng
     */
    @Query(
            value = "SELECT p.id, p.title, p.price, p.status, " +
                    "c.name AS categoryName, " +
                    "m.nickname AS sellerNickname, " +
                    "(SELECT pi.image_url FROM product_image pi WHERE pi.product_id = p.id LIMIT 1) AS thumbnailUrl, " +
                    "p.location_name AS locationName, " +
                    "ST_Distance_Sphere(p.location, ST_SRID(ST_GeomFromText(CONCAT('POINT(', :lng, ' ', :lat, ')')), 4326)) AS distanceMeters, " +
                    "ST_Y(p.location) AS lat, " +
                    "ST_X(p.location) AS lng " +
                    "FROM product p " +
                    "JOIN category c ON p.category_id = c.id " +
                    "JOIN members m ON p.seller_id = m.id " +
                    "WHERE p.location IS NOT NULL " +
                    "AND p.is_deleted = false " +
                    "AND p.status IN ('SALE', 'RESERVED') " +
                    "AND m.deleted = false " +
                    "AND ST_Distance_Sphere(p.location, ST_SRID(ST_GeomFromText(CONCAT('POINT(', :lng, ' ', :lat, ')')), 4326)) <= :radiusMeters " +
                    "ORDER BY distanceMeters",
            countQuery = "SELECT COUNT(*) FROM product p " +
                    "JOIN members m ON p.seller_id = m.id " +
                    "WHERE p.location IS NOT NULL " +
                    "AND p.is_deleted = false " +
                    "AND p.status IN ('SALE', 'RESERVED') " +
                    "AND m.deleted = false " +
                    "AND ST_Distance_Sphere(p.location, ST_SRID(ST_GeomFromText(CONCAT('POINT(', :lng, ' ', :lat, ')')), 4326)) <= :radiusMeters",
            nativeQuery = true
    )
    Page<Object[]> findNearbyProducts(
            @Param("lng") double lng,
            @Param("lat") double lat,
            @Param("radiusMeters") double radiusMeters,
            Pageable pageable);
}

