package com.clone.getchu.global.client.kakao.dto;

/**
 * 카카오 API 응답에서 추출한 위도/경도
 * lat = 위도(y), lng = 경도(x)
 */
public record CoordDto(double lat, double lng) {}
