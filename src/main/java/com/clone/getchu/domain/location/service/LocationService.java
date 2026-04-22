package com.clone.getchu.domain.location.service;

import com.clone.getchu.domain.location.dto.request.AddressVerifyRequest;
import com.clone.getchu.domain.location.dto.request.GpsVerifyRequest;
import com.clone.getchu.domain.location.dto.response.LocationVerifyResponse;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.client.kakao.KakaoLocalApiClient;
import com.clone.getchu.global.client.kakao.dto.CoordDto;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    // WGS84 좌표계 (GPS 표준) — MySQL spatial 함수와 단위 일치를 위해 SRID 4326 명시
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final MemberRepository memberRepository;
    private final KakaoLocalApiClient kakaoLocalApiClient;

    /**
     * GPS 좌표 기반 동네 인증
     * 1. 카카오 좌표→행정구역 API로 행정동명 조회
     * 2. Member location(Point), locationName 업데이트
     */
    @Transactional
    public LocationVerifyResponse verifyByGps(Long memberId, GpsVerifyRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        String locationName = kakaoLocalApiClient.coordToRegionName(request.lat(), request.lng());
        Point location = toPoint(request.lat(), request.lng());

        member.updateLocation(location, locationName);
        return LocationVerifyResponse.from(member);
    }

    /**
     * 주소 텍스트 기반 동네 인증
     * 1. 카카오 주소 검색 API로 위도/경도 조회
     * 2. 카카오 좌표→행정구역 API로 행정동명 조회
     * 3. Member location(Point), locationName 업데이트
     */
    @Transactional
    public LocationVerifyResponse verifyByAddress(Long memberId, AddressVerifyRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        CoordDto coord = kakaoLocalApiClient.addressToCoord(request.address());
        String locationName = kakaoLocalApiClient.coordToRegionName(coord.lat(), coord.lng());
        Point location = toPoint(coord.lat(), coord.lng());

        member.updateLocation(location, locationName);
        return LocationVerifyResponse.from(member);
    }

    // JTS Point 생성: Coordinate(x=경도, y=위도)
    private Point toPoint(double lat, double lng) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    }
}
