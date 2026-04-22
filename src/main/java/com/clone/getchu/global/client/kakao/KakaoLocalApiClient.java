package com.clone.getchu.global.client.kakao;

import com.clone.getchu.global.client.kakao.dto.CoordDto;
import com.clone.getchu.global.client.kakao.dto.KakaoAddressResponse;
import com.clone.getchu.global.client.kakao.dto.KakaoCoordResponse;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.KakaoApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalApiClient {

    private static final String ADDRESS_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String COORD_URL =
            "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json";

    @Value("${kakao.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    /**
     * 주소 문자열 → 위도/경도 변환
     * 도로명 주소가 있으면 우선 사용, 없으면 지번 주소 좌표 반환
     */
    public CoordDto addressToCoord(String address) {
        String url = UriComponentsBuilder.fromHttpUrl(ADDRESS_URL)
                .queryParam("query", address)
                .build()
                .toUriString();

        try {
            ResponseEntity<KakaoAddressResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, buildAuthHeader(), KakaoAddressResponse.class);

            KakaoAddressResponse body = response.getBody();
            if (body == null || body.getDocuments() == null || body.getDocuments().isEmpty()) {
                throw new KakaoApiException(ErrorCode.KAKAO_ADDRESS_NOT_FOUND, address);
            }

            KakaoAddressResponse.Document doc = body.getDocuments().get(0);
            // 도로명 주소 좌표 우선, 없으면 지번 주소 좌표
            KakaoAddressResponse.Coord coord =
                    doc.getRoadAddress() != null ? doc.getRoadAddress() : doc.getAddress();

            if (coord == null) {
                throw new KakaoApiException(ErrorCode.KAKAO_ADDRESS_NOT_FOUND, address);
            }

            return new CoordDto(Double.parseDouble(coord.getY()), Double.parseDouble(coord.getX()));

        } catch (RestClientException e) {
            log.error("카카오 주소 검색 API 호출 실패 [address={}]: {}", address, e.getMessage());
            throw new KakaoApiException(ErrorCode.KAKAO_API_ERROR, e.getMessage());
        }
    }

    /**
     * 위도/경도 → 행정구역 이름 변환
     * 행정동(H) 우선 사용, 없으면 첫 번째 결과 사용
     * 반환 형식: "마포구 합정동"
     */
    public String coordToRegionName(double lat, double lng) {
        // 카카오 API에서 x=경도, y=위도
        String url = UriComponentsBuilder.fromHttpUrl(COORD_URL)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .build()
                .toUriString();

        try {
            ResponseEntity<KakaoCoordResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, buildAuthHeader(), KakaoCoordResponse.class);

            KakaoCoordResponse body = response.getBody();
            if (body == null || body.getDocuments() == null || body.getDocuments().isEmpty()) {
                throw new KakaoApiException(ErrorCode.KAKAO_REGION_NOT_FOUND);
            }

            List<KakaoCoordResponse.Document> docs = body.getDocuments();

            // 행정동(H) 우선, 없으면 첫 번째 문서
            KakaoCoordResponse.Document doc = docs.stream()
                    .filter(d -> "H".equals(d.getRegionType()))
                    .findFirst()
                    .orElse(docs.get(0));

            return doc.getRegion2depthName() + " " + doc.getRegion3depthName();

        } catch (RestClientException e) {
            log.error("카카오 좌표→지역 API 호출 실패 [lat={}, lng={}]: {}", lat, lng, e.getMessage());
            throw new KakaoApiException(ErrorCode.KAKAO_API_ERROR, e.getMessage());
        }
    }

    private HttpEntity<Void> buildAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + apiKey);
        return new HttpEntity<>(headers);
    }
}
