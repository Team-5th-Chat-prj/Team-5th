package com.clone.getchu.domain.product.service;

import com.clone.getchu.domain.category.entity.Category;
import com.clone.getchu.domain.category.repository.CategoryRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.domain.product.dto.*;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, Long sellerId) {
        Member seller = memberRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // 동네 인증 없이는 상품 등록 불가 — 위치 정보가 없으면 근처 상품 기능이 동작하지 않음
        if (seller.getLocation() == null) {
            throw new BusinessException(ErrorCode.LOCATION_NOT_VERIFIED);
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .location(seller.getLocation())
                .locationName(seller.getLocationName())
                .status(ProductEnum.SALE)
                .build();

        product.updateImages(request.imageUrls());
        // 판매자의 현재 인증된 위치를 상품에 복사 — 상품은 등록 당시 동네 기준으로 노출됨
        product.updateLocation(seller.getLocation(), seller.getLocationName());

        productRepository.save(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, Long memberId) {
        Product product = findActiveProduct(productId);

        validateSeller(product, memberId);

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        product.updateProduct(
                request.title(),
                request.description(),
                request.price(),
                request.status(),
                category,
                request.imageUrls()
        );

        return ProductResponse.from(product);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = findActiveProduct(productId);
        return ProductResponse.from(product);
    }

    public CursorPageResponse<ProductListResponse> searchProducts(ProductSearchCondition cond, Pageable pageable) {
        ProductSearchCondition searchCond = applyDefaultStatus(cond);

        CursorPageResponse<Product> productSlice = productRepository.searchByCursor(searchCond, pageable);

        List<ProductListResponse> dtoList = productSlice.getContent().stream()
                .map(ProductListResponse::from)
                .toList();

        return new CursorPageResponse<>(
                dtoList,
                productSlice.getNextCursor(),
                productSlice.isHasNext()
        );
    }

    @Transactional
    public void deleteProduct(Long productId, Long memberId) {
        // [수정] 이미 삭제된 상품을 또 삭제하지 않도록 검증
        Product product = findActiveProduct(productId);

        validateSeller(product, memberId);

        product.softDelete();
    }

    public CursorPageResponse<ProductListResponse> getMyProducts(Long memberId, ProductEnum status, String cursor, Pageable pageable) {
        // 1. 데이터 조회
        CursorPageResponse<Product> result = productRepository.findMyProducts(memberId, status, cursor, pageable);

        // 2. Entity -> DTO 변환
        List<ProductListResponse> responseDtos = result.getContent().stream()
                .map(ProductListResponse::from)
                .toList();

        return new CursorPageResponse<>(responseDtos, result.getNextCursor(), result.isHasNext());
    }

    // --- Helper Methods ---

    private Product findActiveProduct(Long productId) {
        return productRepository.findDetailById(productId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateSeller(Product product, Long memberId) {
        if (!product.getSeller().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
    private ProductSearchCondition applyDefaultStatus(ProductSearchCondition cond) {
        if (cond.status() == null) {
            // Record라면 새로운 객체 생성, 일반 클래스라면 setter나 copy 메서드 활용
            return new ProductSearchCondition(
                    cond.keyword(),
                    cond.categoryId(),
                    ProductEnum.SALE,
                    cond.cursor()
            );
        }
        return cond;
    }

    /**
     * 반경 내 근처 상품 목록 조회 — 거리 오름차순, 페이지당 20개
     *
     * 1단계: native query로 반경 내 상품 ID + 거리(meters) 조회 (페이징 포함)
     * 2단계: JOIN FETCH로 Product 엔티티 일괄 로드 (N+1 방지)
     * 3단계: 1단계 순서를 유지하면서 거리(km, 소수점 1자리) 매핑
     */
    public Page<NearbyProductResponse> getNearbyProducts(
            double lat, double lng, int radiusKm, Pageable pageable) {

        // WKT POINT 형식: x=경도, y=위도 순서 (WGS84)
        String wktPoint = "POINT(" + lng + " " + lat + ")";
        double radiusMeters = radiusKm * 1000.0;

        // 1단계: ID + 거리(meters) 페이지 조회
        Page<Object[]> idsPage = productRepository.findNearbyIdsAndDistance(
                wktPoint, radiusMeters, pageable);

        if (idsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2단계: ID → 거리(km) 맵 — LinkedHashMap으로 거리 오름차순 순서 보존
        Map<Long, Double> idToDistanceKm = new LinkedHashMap<>();
        for (Object[] row : idsPage.getContent()) {
            Long id = ((Number) row[0]).longValue();
            double distanceMeters = ((Number) row[1]).doubleValue();
            // 소수점 1자리 반올림
            double distanceKm = Math.round(distanceMeters / 1000.0 * 10.0) / 10.0;
            idToDistanceKm.put(id, distanceKm);
        }

        // 3단계: JOIN FETCH로 엔티티 일괄 로드
        List<Long> ids = new ArrayList<>(idToDistanceKm.keySet());
        Map<Long, Product> productMap = productRepository.findAllWithDetailsByIds(ids)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4단계: 거리 순서 유지하면서 Response 생성
        List<NearbyProductResponse> content = ids.stream()
                .filter(productMap::containsKey)
                .map(id -> NearbyProductResponse.of(productMap.get(id), idToDistanceKm.get(id)))
                .toList();

        return new PageImpl<>(content, pageable, idsPage.getTotalElements());
    }
}