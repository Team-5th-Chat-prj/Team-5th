package com.clone.getchu.global.client.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카카오 좌표→행정구역 변환 API 응답
 * GET /v2/local/geo/coord2regioncode.json?x={경도}&y={위도}
 *
 * documents 배열에 법정동(B)과 행정동(H) 두 항목이 반환됨.
 * 사용자에게 친숙한 행정동(H)을 우선 사용.
 */
@Getter
@NoArgsConstructor
public class KakaoCoordResponse {

    private List<Document> documents;

    @Getter
    @NoArgsConstructor
    public static class Document {

        @JsonProperty("region_type")
        private String regionType; // "B"=법정동, "H"=행정동

        @JsonProperty("region_2depth_name")
        private String region2depthName; // 예: "마포구"

        @JsonProperty("region_3depth_name")
        private String region3depthName; // 예: "합정동"
    }
}
