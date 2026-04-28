package com.clone.getchu.domain.location.controller;

import com.clone.getchu.domain.location.dto.request.AddressVerifyRequest;
import com.clone.getchu.domain.location.dto.request.GpsVerifyRequest;
import com.clone.getchu.domain.location.dto.response.LocationVerifyResponse;
import com.clone.getchu.domain.location.service.LocationService;
import com.clone.getchu.support.RestDocsSupport;
import com.clone.getchu.support.WithMockCustomUser;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest extends RestDocsSupport {

    @MockBean
    private LocationService locationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final LocationVerifyResponse RESPONSE =
            new LocationVerifyResponse("마포구 합정동", 3);

    // ────────────────────────────── GPS 인증 ──────────────────────────────────

    @Test
    @DisplayName("GPS 동네 인증 성공 - POST /api/location/verify/gps")
    @WithMockCustomUser(memberId = 1L)
    void verifyByGps_success() throws Exception {
        // given
        given(locationService.verifyByGps(anyLong(), any(GpsVerifyRequest.class)))
                .willReturn(RESPONSE);

        GpsVerifyRequest request = new GpsVerifyRequest(37.549, 126.914);

        // when & then
        mockMvc.perform(post("/api/location/verify/gps")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.locationName").value("마포구 합정동"))
                .andExpect(jsonPath("$.data.locationRadius").value(3))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "location/verify-gps",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Location")
                                .summary("GPS 기반 동네 인증")
                                .description("GPS 좌표를 카카오 API로 행정구역명으로 변환하여 회원의 동네를 인증합니다.")
                                .requestSchema(Schema.schema("GpsVerifyRequest"))
                                .responseSchema(Schema.schema("LocationVerifyResponse"))
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .requestFields(
                                        fieldWithPath("lat").type(JsonFieldType.NUMBER)
                                                .description("위도 (-90.0 ~ 90.0)"),
                                        fieldWithPath("lng").type(JsonFieldType.NUMBER)
                                                .description("경도 (-180.0 ~ 180.0)")
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("동네 인증이 완료되었습니다."),
                                        fieldWithPath("data.locationName").type(JsonFieldType.STRING)
                                                .description("인증된 행정동명 (예: 마포구 합정동)"),
                                        fieldWithPath("data.locationRadius").type(JsonFieldType.NUMBER)
                                                .description("근처 상품 조회 반경 km (기본값 3)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("GPS 인증 - lat 범위 초과 시 400 반환")
    @WithMockCustomUser(memberId = 1L)
    void verifyByGps_invalidLat() throws Exception {
        GpsVerifyRequest request = new GpsVerifyRequest(91.0, 126.914);

        mockMvc.perform(post("/api/location/verify/gps")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ────────────────────────────── 주소 인증 ────────────────────────────────

    @Test
    @DisplayName("주소 기반 동네 인증 성공 - POST /api/location/verify/address")
    @WithMockCustomUser(memberId = 1L)
    void verifyByAddress_success() throws Exception {
        // given
        given(locationService.verifyByAddress(anyLong(), any(AddressVerifyRequest.class)))
                .willReturn(RESPONSE);

        AddressVerifyRequest request = new AddressVerifyRequest("서울 마포구 합정동");

        // when & then
        mockMvc.perform(post("/api/location/verify/address")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.locationName").value("마포구 합정동"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "location/verify-address",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Location")
                                .summary("주소 기반 동네 인증")
                                .description("주소 텍스트를 카카오 API로 좌표 변환 후 행정구역명으로 변환하여 동네를 인증합니다.")
                                .requestSchema(Schema.schema("AddressVerifyRequest"))
                                .responseSchema(Schema.schema("LocationVerifyResponse"))
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .requestFields(
                                        fieldWithPath("address").type(JsonFieldType.STRING)
                                                .description("인증할 주소 텍스트 (최대 200자)")
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("동네 인증이 완료되었습니다."),
                                        fieldWithPath("data.locationName").type(JsonFieldType.STRING)
                                                .description("인증된 행정동명 (예: 마포구 합정동)"),
                                        fieldWithPath("data.locationRadius").type(JsonFieldType.NUMBER)
                                                .description("근처 상품 조회 반경 km (기본값 3)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("주소 인증 - 주소 공백이면 400 반환")
    @WithMockCustomUser(memberId = 1L)
    void verifyByAddress_blankAddress() throws Exception {
        AddressVerifyRequest request = new AddressVerifyRequest("");

        mockMvc.perform(post("/api/location/verify/address")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
