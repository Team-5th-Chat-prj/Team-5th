package com.clone.getchu.domain.location.service;

import com.clone.getchu.domain.location.dto.request.AddressVerifyRequest;
import com.clone.getchu.domain.location.dto.request.GpsVerifyRequest;
import com.clone.getchu.domain.location.dto.response.LocationVerifyResponse;
import com.clone.getchu.global.client.kakao.KakaoLocalApiClient;
import com.clone.getchu.global.client.kakao.dto.CoordDto;
import com.clone.getchu.global.exception.KakaoApiException;
import com.clone.getchu.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private LocationUpdater locationUpdater;

    @Mock
    private KakaoLocalApiClient kakaoLocalApiClient;

    private static final Long MEMBER_ID = 1L;
    private static final double LAT = 37.549;
    private static final double LNG = 126.914;
    private static final String REGION_NAME = "마포구 합정동";

    // ────────────────────────────── verifyByGps ──────────────────────────────

    @Test
    @DisplayName("GPS 인증 성공 - 카카오 API 호출 후 locationUpdater에 올바른 좌표를 전달한다")
    void verifyByGps_success() {
        // given
        LocationVerifyResponse expected = new LocationVerifyResponse(REGION_NAME, 3);
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG)).willReturn(REGION_NAME);
        given(locationUpdater.update(eq(MEMBER_ID), any(Point.class), eq(REGION_NAME)))
                .willReturn(expected);

        // when
        LocationVerifyResponse response =
                locationService.verifyByGps(MEMBER_ID, new GpsVerifyRequest(LAT, LNG));

        // then
        assertThat(response.locationName()).isEqualTo(REGION_NAME);
        assertThat(response.locationRadius()).isEqualTo(3);

        verify(kakaoLocalApiClient).coordToRegionName(LAT, LNG);
        verify(locationUpdater).update(
                eq(MEMBER_ID),
                argThat(p -> p != null && p.getX() == LNG && p.getY() == LAT),
                eq(REGION_NAME)
        );
    }

    @Test
    @DisplayName("GPS 인증 - 카카오 API 실패 시 locationUpdater는 호출되지 않는다")
    void verifyByGps_kakaoApiFails() {
        // given
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG))
                .willThrow(KakaoApiException.class);

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByGps(MEMBER_ID, new GpsVerifyRequest(LAT, LNG)))
                .isInstanceOf(KakaoApiException.class);

        verify(locationUpdater, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("GPS 인증 - 존재하지 않는 회원이면 NotFoundException 발생")
    void verifyByGps_memberNotFound() {
        // given
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG)).willReturn(REGION_NAME);
        given(locationUpdater.update(eq(MEMBER_ID), any(Point.class), eq(REGION_NAME)))
                .willThrow(NotFoundException.class);

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByGps(MEMBER_ID, new GpsVerifyRequest(LAT, LNG)))
                .isInstanceOf(NotFoundException.class);
    }

    // ────────────────────────────── verifyByAddress ──────────────────────────

    @Test
    @DisplayName("주소 인증 성공 - addressToCoord → coordToRegionName → locationUpdater 호출")
    void verifyByAddress_success() {
        // given
        LocationVerifyResponse expected = new LocationVerifyResponse(REGION_NAME, 3);
        given(kakaoLocalApiClient.addressToCoord("서울 마포구 합정동"))
                .willReturn(new CoordDto(LAT, LNG));
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG)).willReturn(REGION_NAME);
        given(locationUpdater.update(eq(MEMBER_ID), any(Point.class), eq(REGION_NAME)))
                .willReturn(expected);

        // when
        LocationVerifyResponse response =
                locationService.verifyByAddress(MEMBER_ID, new AddressVerifyRequest("서울 마포구 합정동"));

        // then
        assertThat(response.locationName()).isEqualTo(REGION_NAME);
        verify(kakaoLocalApiClient).addressToCoord("서울 마포구 합정동");
        verify(kakaoLocalApiClient).coordToRegionName(LAT, LNG);
        verify(locationUpdater).update(
                eq(MEMBER_ID),
                argThat(p -> p != null && p.getX() == LNG && p.getY() == LAT),
                eq(REGION_NAME)
        );
    }

    @Test
    @DisplayName("주소 인증 - 주소 검색 실패 시 coordToRegionName과 locationUpdater는 호출되지 않는다")
    void verifyByAddress_addressToCoordFails() {
        // given
        given(kakaoLocalApiClient.addressToCoord(anyString()))
                .willThrow(KakaoApiException.class);

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByAddress(MEMBER_ID, new AddressVerifyRequest("존재하지않는주소xyz")))
                .isInstanceOf(KakaoApiException.class);

        verify(kakaoLocalApiClient, never()).coordToRegionName(anyDouble(), anyDouble());
        verify(locationUpdater, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("주소 인증 - 존재하지 않는 회원이면 NotFoundException 발생")
    void verifyByAddress_memberNotFound() {
        // given
        given(kakaoLocalApiClient.addressToCoord(anyString()))
                .willReturn(new CoordDto(LAT, LNG));
        given(kakaoLocalApiClient.coordToRegionName(LAT, LNG)).willReturn(REGION_NAME);
        given(locationUpdater.update(eq(MEMBER_ID), any(Point.class), eq(REGION_NAME)))
                .willThrow(NotFoundException.class);

        // when & then
        assertThatThrownBy(() ->
                locationService.verifyByAddress(MEMBER_ID, new AddressVerifyRequest("서울")))
                .isInstanceOf(NotFoundException.class);
    }
}
