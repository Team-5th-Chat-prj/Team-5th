package com.clone.getchu.global.client.kakao;

import com.clone.getchu.global.client.kakao.dto.CoordDto;
import com.clone.getchu.global.exception.KakaoApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KakaoLocalApiClientTest {

    private MockRestServiceServer mockServer;
    private KakaoLocalApiClient kakaoLocalApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        kakaoLocalApiClient = new KakaoLocalApiClient(restClient);
        ReflectionTestUtils.setField(kakaoLocalApiClient, "apiKey", "test-api-key");
    }

    // ────────────────────────────── addressToCoord ──────────────────────────────

    @Test
    @DisplayName("addressToCoord - road_address 좌표 우선 반환")
    void addressToCoord_success() {
        // given
        String responseBody = """
                {
                  "documents": [
                    {
                      "address":      { "x": "126.914", "y": "37.549" },
                      "road_address": { "x": "126.915", "y": "37.550" }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("dapi.kakao.com/v2/local/search/address.json")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "KakaoAK test-api-key"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        CoordDto result = kakaoLocalApiClient.addressToCoord("서울 마포구 합정동");

        // then
        assertThat(result.lat()).isEqualTo(37.550);   // road_address y = 위도
        assertThat(result.lng()).isEqualTo(126.915);  // road_address x = 경도
        mockServer.verify();
    }

    @Test
    @DisplayName("addressToCoord - road_address null 이면 address 좌표 사용")
    void addressToCoord_fallbackToAddress() {
        // given
        String responseBody = """
                {
                  "documents": [
                    {
                      "address":      { "x": "126.914", "y": "37.549" },
                      "road_address": null
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("dapi.kakao.com/v2/local/search/address.json")))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        CoordDto result = kakaoLocalApiClient.addressToCoord("합정동");

        // then
        assertThat(result.lat()).isEqualTo(37.549);
        assertThat(result.lng()).isEqualTo(126.914);
    }

    @Test
    @DisplayName("addressToCoord - 검색 결과 없으면 KakaoApiException (KAKAO_ADDRESS_NOT_FOUND)")
    void addressToCoord_emptyResult_throwsException() {
        // given
        mockServer.expect(requestTo(containsString("dapi.kakao.com/v2/local/search/address.json")))
                .andRespond(withSuccess("""
                        { "documents": [] }
                        """, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> kakaoLocalApiClient.addressToCoord("존재하지않는주소xyz"))
                .isInstanceOf(KakaoApiException.class);
    }

    @Test
    @DisplayName("addressToCoord - 서버 오류(5xx) 시 KakaoApiException (KAKAO_API_ERROR)")
    void addressToCoord_serverError_throwsException() {
        // given
        mockServer.expect(requestTo(containsString("dapi.kakao.com/v2/local/search/address.json")))
                .andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> kakaoLocalApiClient.addressToCoord("서울 마포구"))
                .isInstanceOf(KakaoApiException.class);
    }

    // ────────────────────────────── coordToRegionName ───────────────────────────

    @Test
    @DisplayName("coordToRegionName - 행정동(H) 우선으로 동네명 반환")
    void coordToRegionName_success_prefersH() {
        // given
        String responseBody = """
                {
                  "documents": [
                    {
                      "region_type": "B",
                      "region_2depth_name": "마포구",
                      "region_3depth_name": "합정동"
                    },
                    {
                      "region_type": "H",
                      "region_2depth_name": "마포구",
                      "region_3depth_name": "합정동"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("dapi.kakao.com/v2/local/geo/coord2regioncode.json")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "KakaoAK test-api-key"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        String result = kakaoLocalApiClient.coordToRegionName(37.549, 126.914);

        // then
        assertThat(result).isEqualTo("마포구 합정동");
        mockServer.verify();
    }

    @Test
    @DisplayName("coordToRegionName - 행정동(H) 없으면 첫 번째 document 사용")
    void coordToRegionName_fallbackToFirst() {
        // given
        mockServer.expect(requestTo(containsString("dapi.kakao.com/v2/local/geo/coord2regioncode.json")))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {
                              "region_type": "B",
                              "region_2depth_name": "강남구",
                              "region_3depth_name": "역삼동"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        String result = kakaoLocalApiClient.coordToRegionName(37.500, 127.036);

        // then
        assertThat(result).isEqualTo("강남구 역삼동");
    }

    @Test
    @DisplayName("coordToRegionName - 서버 오류(5xx) 시 KakaoApiException 발생")
    void coordToRegionName_serverError_throwsException() {
        // given
        mockServer.expect(requestTo(containsString("dapi.kakao.com/v2/local/geo/coord2regioncode.json")))
                .andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> kakaoLocalApiClient.coordToRegionName(37.549, 126.914))
                .isInstanceOf(KakaoApiException.class);
    }
}
