package com.clone.getchu.domain.product.service;

import com.clone.getchu.domain.category.entity.Category;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.product.dto.NearbyProductResponse;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NearbyProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private com.clone.getchu.domain.category.repository.CategoryRepository categoryRepository;

    @Mock
    private com.clone.getchu.domain.member.repository.MemberRepository memberRepository;

    private static final double LAT = 37.549;
    private static final double LNG = 126.914;
    private static final int RADIUS_KM = 3;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    // ── 픽스처 헬퍼 ────────────────────────────────────────────────────────────

    private Object[] idDistanceRow(long id, double distanceMeters) {
        return new Object[]{id, distanceMeters};
    }

    private Product mockProduct(long id, String title, String locationName) {
        Product product = mock(Product.class);
        Member seller = mock(Member.class);
        Category category = mock(Category.class);

        given(product.getId()).willReturn(id);
        given(product.getTitle()).willReturn(title);
        given(product.getPrice()).willReturn(10000);
        given(product.getStatus()).willReturn(ProductEnum.SALE);
        given(product.getLocationName()).willReturn(locationName);
        given(product.getSeller()).willReturn(seller);
        given(seller.getNickname()).willReturn("seller" + id);
        given(product.getCategory()).willReturn(category);
        given(category.getName()).willReturn("전자기기");
        given(product.getImages()).willReturn(List.of());
        return product;
    }

    // ── 테스트 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("반경 내 상품만 조회되고 총 개수가 정확하다")
    void getNearbyProducts_returnsOnlyMatchingProducts() {
        // given
        List<Object[]> rows = List.of(
                idDistanceRow(1L, 500.0),
                idDistanceRow(2L, 1200.0)
        );
        Page<Object[]> idsPage = new PageImpl<>(rows, PAGEABLE, 2);
        given(productRepository.findNearbyIdsAndDistance(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(idsPage);

        Product p1 = mockProduct(1L, "상품A", "마포구 합정동");
        Product p2 = mockProduct(2L, "상품B", "마포구 망원동");
        given(productRepository.findAllWithDetailsByIds(List.of(1L, 2L)))
                .willReturn(List.of(p1, p2));

        // when
        Page<NearbyProductResponse> result =
                productService.getNearbyProducts(LAT, LNG, RADIUS_KM, PAGEABLE);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
        assertThat(result.getContent().get(1).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("거리 오름차순 순서가 유지된다")
    void getNearbyProducts_preservesDistanceOrder() {
        // given — 가까운 순서: id=3(100m), id=1(800m), id=2(2500m)
        List<Object[]> rows = List.of(
                idDistanceRow(3L, 100.0),
                idDistanceRow(1L, 800.0),
                idDistanceRow(2L, 2500.0)
        );
        Page<Object[]> idsPage = new PageImpl<>(rows, PAGEABLE, 3);
        given(productRepository.findNearbyIdsAndDistance(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(idsPage);

        Product p1 = mockProduct(1L, "상품1", "A동");
        Product p2 = mockProduct(2L, "상품2", "B동");
        Product p3 = mockProduct(3L, "상품3", "C동");
        given(productRepository.findAllWithDetailsByIds(List.of(3L, 1L, 2L)))
                .willReturn(List.of(p1, p2, p3));

        // when
        Page<NearbyProductResponse> result =
                productService.getNearbyProducts(LAT, LNG, RADIUS_KM, PAGEABLE);

        // then — 응답 순서가 native query(거리 오름차순) 기준이어야 한다
        List<Long> ids = result.getContent().stream().map(NearbyProductResponse::id).toList();
        assertThat(ids).containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("거리(meters)를 km 소수점 1자리로 올바르게 변환한다")
    void getNearbyProducts_distanceConvertedCorrectly() {
        // given — 1234m → 1.2km (반올림), 567m → 0.6km
        List<Object[]> rows = List.of(
                idDistanceRow(1L, 1234.0),
                idDistanceRow(2L, 567.0)
        );
        Page<Object[]> idsPage = new PageImpl<>(rows, PAGEABLE, 2);
        given(productRepository.findNearbyIdsAndDistance(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(idsPage);

        Product p1 = mockProduct(1L, "상품1", "A동");
        Product p2 = mockProduct(2L, "상품2", "B동");
        given(productRepository.findAllWithDetailsByIds(List.of(1L, 2L)))
                .willReturn(List.of(p1, p2));

        // when
        Page<NearbyProductResponse> result =
                productService.getNearbyProducts(LAT, LNG, RADIUS_KM, PAGEABLE);

        // then
        assertThat(result.getContent().get(0).distanceKm()).isEqualTo(1.2);
        assertThat(result.getContent().get(1).distanceKm()).isEqualTo(0.6);
    }

    @Test
    @DisplayName("반경 내 상품이 없으면 빈 페이지를 반환한다")
    void getNearbyProducts_emptyResult() {
        // given
        given(productRepository.findNearbyIdsAndDistance(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(Page.empty(PAGEABLE));

        // when
        Page<NearbyProductResponse> result =
                productService.getNearbyProducts(LAT, LNG, RADIUS_KM, PAGEABLE);

        // then
        assertThat(result.isEmpty()).isTrue();
        verify(productRepository, never()).findAllWithDetailsByIds(any());
    }

    @Test
    @DisplayName("lng, lat, radiusMeters가 올바른 값으로 repository에 전달된다")
    void getNearbyProducts_passesCorrectParamsToRepository() {
        // given
        given(productRepository.findNearbyIdsAndDistance(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(Page.empty(PAGEABLE));

        // when
        productService.getNearbyProducts(LAT, LNG, RADIUS_KM, PAGEABLE);

        // then — 경도(lng), 위도(lat), 반경(meters) 순서로 전달
        verify(productRepository).findNearbyIdsAndDistance(
                eq(LNG),
                eq(LAT),
                eq((double) RADIUS_KM * 1000),
                eq(PAGEABLE)
        );
    }
}
