package com.clone.getchu.domain.location.service;

import com.clone.getchu.domain.location.dto.request.AddressVerifyRequest;
import com.clone.getchu.domain.location.dto.request.GpsVerifyRequest;
import com.clone.getchu.domain.location.dto.response.LocationVerifyResponse;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.client.kakao.KakaoLocalApiClient;
import com.clone.getchu.global.client.kakao.dto.CoordDto;
import com.clone.getchu.global.exception.KakaoApiException;
import com.clone.getchu.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @InjectMocks
    private LocationService locationService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private KakaoLocalApiClient kakaoLocalApiClient;

    private static final Long MEMBER_ID = 1L;
    private static final double LAT = 37.549;
    private static final double LNG = 126.914;
    private static final String REGION_NAME = "마포구 합정동";

    private Member buildMember() {
        return Member.builder()
                .email("test@test.com")
                .password("encoded")
                .nickname("테스터")
                .build();
    }

    // ────────────────────────────── verifyByGps ──────────────────────────────

    @Test
    @DisplayName("GPS 인증 성공 - location과 locationName이 업데이트된다")
    void verifyByGps_success() {
        // given
        Member member = spy(buildMember());
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG)).willReturn(REGION_NAME);

        // when
        LocationVerifyResponse response =
                locationService.verifyByGps(MEMBER_ID, new GpsVerifyRequest(LAT, LNG));

        // then
        assertThat(response.locationName()).isEqualTo(REGION_NAME);
        assertThat(response.locationRadius()).isEqualTo(3); // 기본값

        verify(member).updateLocation(argThat(p -> p != null
                && p.getX() == LNG  // JTS: x=경도
                && p.getY() == LAT  // JTS: y=위도
        ), eq(REGION_NAME));
        verify(kakaoLocalApiClient).coordToRegionName(LAT, LNG);
    }

    @Test
    @DisplayName("GPS 인증 - 존재하지 않는 회원이면 NotFoundException 발생")
    void verifyByGps_memberNotFound() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByGps(MEMBER_ID, new GpsVerifyRequest(LAT, LNG)))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(kakaoLocalApiClient);
    }

    @Test
    @DisplayName("GPS 인증 - 카카오 API 실패 시 KakaoApiException 전파")
    void verifyByGps_kakaoApiFails() {
        // given
        Member member = buildMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG))
                .willThrow(KakaoApiException.class);

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByGps(MEMBER_ID, new GpsVerifyRequest(LAT, LNG)))
                .isInstanceOf(KakaoApiException.class);
    }

    // ────────────────────────────── verifyByAddress ──────────────────────────

    @Test
    @DisplayName("주소 인증 성공 - addressToCoord → coordToRegionName → location 업데이트")
    void verifyByAddress_success() {
        // given
        Member member = spy(buildMember());
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(kakaoLocalApiClient.addressToCoord("서울 마포구 합정동"))
                .willReturn(new CoordDto(LAT, LNG));
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG)).willReturn(REGION_NAME);

        // when
        LocationVerifyResponse response =
                locationService.verifyByAddress(MEMBER_ID, new AddressVerifyRequest("서울 마포구 합정동"));

        // then
        assertThat(response.locationName()).isEqualTo(REGION_NAME);
        assertThat(response.locationRadius()).isEqualTo(3);

        verify(kakaoLocalApiClient).addressToCoord("서울 마포구 합정동");
        verify(kakaoLocalApiClient).coordToRegionName(LAT, LNG);
        verify(member).updateLocation(argThat(p -> p != null
                && p.getX() == LNG
                && p.getY() == LAT
        ), eq(REGION_NAME));
    }

    @Test
    @DisplayName("주소 인증 - 주소 검색 실패 시 KakaoApiException 전파")
    void verifyByAddress_addressToCoordFails() {
        // given
        Member member = buildMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(kakaoLocalApiClient.addressToCoord(anyString()))
                .willThrow(KakaoApiException.class);

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByAddress(MEMBER_ID, new AddressVerifyRequest("존재하지않는주소xyz")))
                .isInstanceOf(KakaoApiException.class);

        verify(kakaoLocalApiClient, never()).coordToRegionName(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("주소 인증 - 존재하지 않는 회원이면 NotFoundException 발생")
    void verifyByAddress_memberNotFound() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByAddress(MEMBER_ID, new AddressVerifyRequest("서울")))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(kakaoLocalApiClient);
    }
}
