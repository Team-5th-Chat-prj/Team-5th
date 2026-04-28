package com.clone.getchu.domain.product.service;

import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.product.dto.NearbyProductResponse;
import com.clone.getchu.domain.product.dto.NearbyProductRow;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private static final long MEMBER_ID = 1L;
    private static final double LAT = 37.549;
    private static final double LNG = 126.914;
    private static final int RADIUS_KM = 3;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    // ── 픽스처 헬퍼 ────────────────────────────────────────────────────────────

    private Member mockMemberWithLocation() {
        Member member = mock(Member.class);
        Point point = GF.createPoint(new Coordinate(LNG, LAT));
        given(member.getLocation()).willReturn(point);
        given(member.getLocationRadius()).willReturn(RADIUS_KM);
        return member;
    }

    private Member mockMemberWithoutLocation() {
        Member member = mock(Member.class);
        given(member.getLocation()).willReturn(null);
        return member;
    }

    private NearbyProductRow mockRow(long id, String title, double distanceMeters) {
        NearbyProductRow row = mock(NearbyProductRow.class);
        given(row.getId()).willReturn(id);
        given(row.getTitle()).willReturn(title);
        given(row.getPrice()).willReturn(10000);
        given(row.getStatus()).willReturn(ProductEnum.SALE.name());
        given(row.getCategoryName()).willReturn("전자기기");
        given(row.getSellerNickname()).willReturn("seller" + id);
        given(row.getThumbnailUrl()).willReturn("https://cdn.example.com/img" + id + ".jpg");
        given(row.getLocationName()).willReturn("마포구 합정동");
        given(row.getDistanceMeters()).willReturn(distanceMeters);
        given(row.getLat()).willReturn(LAT);
        given(row.getLng()).willReturn(LNG);
        return row;
    }

    // ── 테스트 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("반경 내 상품만 조회되고 총 개수가 정확하다")
    void getNearbyProducts_returnsOnlyMatchingProducts() {
        // given
        Member member = mockMemberWithLocation();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        List<NearbyProductRow> rows = List.of(mockRow(1L, "상품A", 500.0), mockRow(2L, "상품B", 1200.0));
        given(productRepository.findNearbyProducts(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(new PageImpl<>(rows, PAGEABLE, 2));

        // when
        Page<NearbyProductResponse> result = productService.getNearbyProducts(MEMBER_ID, PAGEABLE);

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
        Member member = mockMemberWithLocation();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        List<NearbyProductRow> rows = List.of(
                mockRow(3L, "상품3", 100.0),
                mockRow(1L, "상품1", 800.0),
                mockRow(2L, "상품2", 2500.0)
        );
        given(productRepository.findNearbyProducts(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(new PageImpl<>(rows, PAGEABLE, 3));

        // when
        Page<NearbyProductResponse> result = productService.getNearbyProducts(MEMBER_ID, PAGEABLE);

        // then
        List<Long> ids = result.getContent().stream().map(NearbyProductResponse::id).toList();
        assertThat(ids).containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("거리(meters)를 km 소수점 1자리로 올바르게 변환한다")
    void getNearbyProducts_distanceConvertedCorrectly() {
        // given — 1234m → 1.2km (반올림), 567m → 0.6km
        Member member = mockMemberWithLocation();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        List<NearbyProductRow> rows = List.of(mockRow(1L, "상품1", 1234.0), mockRow(2L, "상품2", 567.0));
        given(productRepository.findNearbyProducts(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(new PageImpl<>(rows, PAGEABLE, 2));

        // when
        Page<NearbyProductResponse> result = productService.getNearbyProducts(MEMBER_ID, PAGEABLE);

        // then
        assertThat(result.getContent().get(0).distanceKm()).isEqualTo(1.2);
        assertThat(result.getContent().get(1).distanceKm()).isEqualTo(0.6);
    }

    @Test
    @DisplayName("반경 내 상품이 없으면 빈 페이지를 반환한다")
    void getNearbyProducts_emptyResult() {
        // given
        Member member = mockMemberWithLocation();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(productRepository.findNearbyProducts(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(Page.empty(PAGEABLE));

        // when
        Page<NearbyProductResponse> result = productService.getNearbyProducts(MEMBER_ID, PAGEABLE);

        // then
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("동네 인증이 안 된 회원은 LOCATION_NOT_VERIFIED 예외가 발생한다")
    void getNearbyProducts_locationNotVerified() {
        // given
        Member member = mockMemberWithoutLocation();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> productService.getNearbyProducts(MEMBER_ID, PAGEABLE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOCATION_NOT_VERIFIED);

        verify(productRepository, never()).findNearbyProducts(anyDouble(), anyDouble(), anyDouble(), any());
    }

    @Test
    @DisplayName("lng, lat, locationRadius가 repository에 올바르게 전달된다")
    void getNearbyProducts_passesCorrectParamsToRepository() {
        // given
        Member member = mockMemberWithLocation();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(productRepository.findNearbyProducts(anyDouble(), anyDouble(), anyDouble(), eq(PAGEABLE)))
                .willReturn(Page.empty(PAGEABLE));

        // when
        productService.getNearbyProducts(MEMBER_ID, PAGEABLE);

        // then
        verify(productRepository).findNearbyProducts(eq(LNG), eq(LAT), eq((double) RADIUS_KM * 1000), eq(PAGEABLE));
    }
}
