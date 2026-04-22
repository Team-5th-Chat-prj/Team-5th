package com.clone.getchu.global.client.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카카오 주소 검색 API 응답
 * GET /v2/local/search/address.json?query={주소}
 */
@Getter
@NoArgsConstructor
public class KakaoAddressResponse {

    private List<Document> documents;

    @Getter
    @NoArgsConstructor
    public static class Document {

        // 지번/도로명 좌표 — 둘 중 하나는 항상 존재
        private Coord address;

        // road_address는 도로명 주소가 없으면 null
        @JsonProperty("road_address")
        private Coord roadAddress;
    }

    @Getter
    @NoArgsConstructor
    public static class Coord {
        private String x; // 경도 (longitude)
        private String y; // 위도 (latitude)
    }
}
