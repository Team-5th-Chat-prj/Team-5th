package com.clone.getchu.domain.product.repository;

import com.clone.getchu.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findDetailById(@Param("id") Long id);

    /**
     * 반경 내 상품의 ID와 거리(미터)만 조회 — 거리 오름차순 정렬, 페이징 포함
     * POINT(경도 위도) 순서 주의 (WKT 표준: x=경도, y=위도)
     * location IS NULL 인 상품 자동 제외
     */
    @Query(
            value = "SELECT p.id, " +
                    "ST_Distance_Sphere(p.location, ST_PointFromText(:point, 4326, 'axis-order=long-lat')) " +
                    "FROM product p " +
                    "WHERE p.location IS NOT NULL " +
                    "AND p.is_deleted = false " +
                    "AND p.status IN ('SALE', 'RESERVED') " +
                    "AND ST_Distance_Sphere(p.location, ST_PointFromText(:point, 4326, 'axis-order=long-lat')) <= :radiusMeters " +
                    "ORDER BY ST_Distance_Sphere(p.location, ST_PointFromText(:point, 4326, 'axis-order=long-lat'))",
            countQuery = "SELECT COUNT(*) FROM product p " +
                    "WHERE p.location IS NOT NULL " +
                    "AND p.is_deleted = false " +
                    "AND p.status IN ('SALE', 'RESERVED') " +
                    "AND ST_Distance_Sphere(p.location, ST_PointFromText(:point, 4326, 'axis-order=long-lat')) <= :radiusMeters",
            nativeQuery = true
    )
    Page<Object[]> findNearbyIdsAndDistance(
            @Param("point") String point,
            @Param("radiusMeters") double radiusMeters,
            Pageable pageable);

    /**
     * ID 목록으로 상품 엔티티를 JOIN FETCH 로드 — N+1 방지
     * DISTINCT: images 컬렉션 JOIN 시 중복 Product 행 제거
     */
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.seller " +
            "LEFT JOIN FETCH p.images " +
            "WHERE p.id IN :ids")
    List<Product> findAllWithDetailsByIds(@Param("ids") List<Long> ids);
}

