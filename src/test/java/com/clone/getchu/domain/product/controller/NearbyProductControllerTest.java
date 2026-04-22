package com.clone.getchu.domain.product.controller;

import com.clone.getchu.domain.product.dto.NearbyProductResponse;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.service.ProductService;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.support.RestDocsSupport;
import com.clone.getchu.support.WithMockCustomUser;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NearbyProductController.class)
class NearbyProductControllerTest extends RestDocsSupport {

    @MockBean
    private ProductService productService;

    // ── 픽스처 ─────────────────────────────────────────────────────────────────

    private NearbyProductResponse sampleResponse(long id, double distanceKm) {
        return new NearbyProductResponse(
                id,
                "중고 노트북 " + id,
                150000,
                ProductEnum.SALE,
                "전자기기",
                "판매자" + id,
                "https://cdn.example.com/img" + id + ".jpg",
                "마포구 합정동",
                distanceKm,
                37.5328,
                126.7247
        );
    }

    // ── 테스트 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("근처 상품 조회 성공 - GET /api/products/nearby")
    @WithMockCustomUser(memberId = 1L)
    void getNearbyProducts_success() throws Exception {
        // given
        List<NearbyProductResponse> content = List.of(
                sampleResponse(1L, 0.5),
                sampleResponse(2L, 1.2)
        );
        Page<NearbyProductResponse> page = new PageImpl<>(content, PageRequest.of(0, 20), 2);
        given(productService.getNearbyProducts(anyLong(), any()))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/products/nearby")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].distanceKm").value(0.5))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "product/nearby-products",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Product")
                                .summary("근처 상품 목록 조회")
                                .description("로그인한 회원의 인증된 동네 위치를 기준으로 반경 내 판매 중인 상품을 거리 오름차순으로 조회합니다.\n" +
                                        "동네 인증이 되어 있지 않으면 400을 반환합니다.")
                                .responseSchema(Schema.schema("NearbyProductResponse"))
                                .queryParameters(
                                        parameterWithName("page").description("페이지 번호 (0부터, 기본값 0)").optional()
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                        fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("상품 ID"),
                                        fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("상품 제목"),
                                        fieldWithPath("data.content[].price").type(JsonFieldType.NUMBER).description("가격"),
                                        fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("상품 상태 (SALE / RESERVE / SOLD)"),
                                        fieldWithPath("data.content[].categoryName").type(JsonFieldType.STRING).description("카테고리명").optional(),
                                        fieldWithPath("data.content[].sellerNickname").type(JsonFieldType.STRING).description("판매자 닉네임"),
                                        fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL (없으면 null)").optional(),
                                        fieldWithPath("data.content[].locationName").type(JsonFieldType.STRING).description("상품 등록 동네명").optional(),
                                        fieldWithPath("data.content[].distanceKm").type(JsonFieldType.NUMBER).description("인증된 동네와의 거리 (km, 소수점 1자리)"),
                                        fieldWithPath("data.content[].lat").type(JsonFieldType.NUMBER).description("상품 위치 위도").optional(),
                                        fieldWithPath("data.content[].lng").type(JsonFieldType.NUMBER).description("상품 위치 경도").optional(),
                                        fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                        fieldWithPath("data.pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                        fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 없음 여부"),
                                        fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                        fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                        fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                                        fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                        fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                        fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지 적용 여부"),
                                        fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지 미적용 여부"),
                                        fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                        fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 상품 수"),
                                        fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                        fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 번째 페이지 여부"),
                                        fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                        fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                        fieldWithPath("data.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                        fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 없음 여부"),
                                        fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                        fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                        fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 상품 수"),
                                        fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("비어있음 여부")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("비로그인 상태에서 401 반환")
    void getNearbyProducts_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/products/nearby"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("동네 인증이 안 된 회원은 400 반환")
    @WithMockCustomUser(memberId = 1L)
    void getNearbyProducts_locationNotVerified_returns400() throws Exception {
        // given
        given(productService.getNearbyProducts(anyLong(), any()))
                .willThrow(new BusinessException(ErrorCode.LOCATION_NOT_VERIFIED));

        // when & then
        mockMvc.perform(get("/api/products/nearby"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("근처 상품이 없으면 빈 페이지 반환")
    @WithMockCustomUser(memberId = 1L)
    void getNearbyProducts_emptyResult() throws Exception {
        // given
        given(productService.getNearbyProducts(anyLong(), any()))
                .willReturn(Page.empty(PageRequest.of(0, 20)));

        // when & then
        mockMvc.perform(get("/api/products/nearby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
