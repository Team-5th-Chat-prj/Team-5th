package com.clone.getchu.domain.location.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * GPS 좌표 기반 동네 인증 요청
 * lat: 위도 (-90.0 ~ 90.0)
 * lng: 경도 (-180.0 ~ 180.0)
 */
public record GpsVerifyRequest(

        @NotNull(message = "위도(lat)는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
        @DecimalMax(value = "90.0",  message = "위도는 90.0 이하이어야 합니다.")
        Double lat,

        @NotNull(message = "경도(lng)는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
        @DecimalMax(value = "180.0",  message = "경도는 180.0 이하이어야 합니다.")
        Double lng
) {}
