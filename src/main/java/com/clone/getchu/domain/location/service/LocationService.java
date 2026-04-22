package com.clone.getchu.domain.location.service;

import com.clone.getchu.domain.location.dto.request.AddressVerifyRequest;
import com.clone.getchu.domain.location.dto.request.GpsVerifyRequest;
import com.clone.getchu.domain.location.dto.response.LocationVerifyResponse;
import com.clone.getchu.global.client.kakao.KakaoLocalApiClient;
import com.clone.getchu.global.client.kakao.dto.CoordDto;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final LocationUpdater locationUpdater;
    private final KakaoLocalApiClient kakaoLocalApiClient;

    /**
     * GPS 좌표 기반 동네 인증
     * 카카오 API 호출(트랜잭션 없음) → DB 저장(LocationUpdater, 짧은 트랜잭션)
     * 외부 API 대기 중에 DB 커넥션을 점유하지 않도록 분리
     */
    public LocationVerifyResponse verifyByGps(Long memberId, GpsVerifyRequest request) {
        String locationName = kakaoLocalApiClient.coordToRegionName(request.lat(), request.lng());
        Point location = toPoint(request.lat(), request.lng());
        return locationUpdater.update(memberId, location, locationName);
    }

    /**
     * 주소 텍스트 기반 동네 인증
     * 카카오 API 호출(트랜잭션 없음) → DB 저장(LocationUpdater, 짧은 트랜잭션)
     */
    public LocationVerifyResponse verifyByAddress(Long memberId, AddressVerifyRequest request) {
        CoordDto coord = kakaoLocalApiClient.addressToCoord(request.address());
        String locationName = kakaoLocalApiClient.coordToRegionName(coord.lat(), coord.lng());
        Point location = toPoint(coord.lat(), coord.lng());
        return locationUpdater.update(memberId, location, locationName);
    }

    private Point toPoint(double lat, double lng) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    }
}
