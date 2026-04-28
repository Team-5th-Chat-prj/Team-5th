package com.clone.getchu.domain.product.controller;

import com.clone.getchu.domain.product.dto.*;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.service.ProductService;
import com.clone.getchu.global.common.CursorPageResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest extends RestDocsSupport {

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품 등록 - POST /products")
    @WithMockCustomUser
    void createProduct() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "아이폰 15 팝니다", "거의 새거에요", 1000000, 1L, List.of("url1", "url2")
        );
        ProductResponse response = new ProductResponse(
                1L, 1L, "아이폰 15 팝니다", "거의 새거에요", 1000000,
                ProductEnum.SALE, 0, "디지털기기", "애플매니아",
                List.of("url1", "url2"), LocalDateTime.of(2024, 1, 1, 0, 0)
        );
        given(productService.createProduct(any(ProductCreateRequest.class), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "product/create-product",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Product")
                                .summary("상품 등록")
                                .description("새 상품을 등록합니다. 로그인한 회원이 판매자가 됩니다.")
                                .requestHeaders(headerWithName("Authorization").description("Bearer JWT 액세스 토큰"))
                                .requestSchema(Schema.schema("ProductCreateRequest"))
                                .responseSchema(Schema.schema("ProductResponse"))
                                .requestFields(
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("상품 제목 (2~40자)"),
                                        fieldWithPath("description").type(JsonFieldType.STRING).description("상품 설명"),
                                        fieldWithPath("price").type(JsonFieldType.NUMBER).description("가격 (100원 이상)"),
                                        fieldWithPath("categoryId").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                        fieldWithPath("imageUrls").type(JsonFieldType.ARRAY).description("상품 이미지 URL 목록").optional()
                                )
                                .responseFields(productResponseFields())
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("상품 목록/검색 조회 - GET /products")
    void getProducts() throws Exception {
        // given
        CursorPageResponse<ProductListResponse> response = new CursorPageResponse<>(
                List.of(new ProductListResponse(1L, "아이폰 15", 1000000, ProductEnum.SALE,
                        "https://img.url/thumb.jpg", 5, LocalDateTime.of(2024, 1, 1, 0, 0))),
                "cursor-abc", false
        );
        given(productService.searchProducts(any(ProductSearchCondition.class), any(Pageable.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/products")
                        .param("keyword", "아이폰")
                        .param("categoryId", "1")
                        .param("status", "SALE")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].title").value("아이폰 15"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "product/get-products",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Product")
                                .summary("상품 목록/검색 조회")
                                .description("전체 상품 목록 조회 및 키워드/카테고리/상태 필터 검색을 지원합니다. 비로그인 상태에서도 조회 가능합니다.")
                                .queryParameters(
                                        parameterWithName("keyword").description("검색 키워드").optional(),
                                        parameterWithName("categoryId").description("카테고리 ID 필터").optional(),
                                        parameterWithName("status").description("상품 상태 필터 (SALE / RESERVED / SOLD)").optional(),
                                        parameterWithName("cursor").description("페이지 커서 (첫 페이지는 생략)").optional(),
                                        parameterWithName("size").description("페이지 크기 (기본값 20)").optional(),
                                        parameterWithName("page").description("페이지 번호").optional(),
                                        parameterWithName("sort").description("정렬 옵션").optional()
                                )
                                .responseSchema(Schema.schema("ProductListPageResponse"))
                                .responseFields(productListPageResponseFields())
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("상품 상세 조회 - GET /products/{productId}")
    void getProduct() throws Exception {
        // given
        ProductResponse response = new ProductResponse(
                1L, 2L, "아이폰 15", "거의 새거에요", 1000000, ProductEnum.SALE, 5,
                "디지털기기", "판매자닉네임", List.of("url1", "url2"),
                LocalDateTime.of(2024, 1, 1, 0, 0)
        );
        given(productService.getProduct(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.title").value("아이폰 15"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "product/get-product",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Product")
                                .summary("상품 상세 조회")
                                .description("상품 ID로 상세 정보를 조회합니다. 비로그인 상태에서도 조회 가능합니다.")
                                .pathParameters(parameterWithName("productId").description("상품 ID"))
                                .responseSchema(Schema.schema("ProductResponse"))
                                .responseFields(productResponseFields())
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("상품 수정 - PATCH /products/{productId}")
    @WithMockCustomUser
    void updateProduct() throws Exception {
        // given
        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 제목", null, 900000, null, ProductEnum.RESERVED, null
        );
        ProductResponse response = new ProductResponse(
                1L, 1L, "수정된 제목", "거의 새거에요", 900000, ProductEnum.RESERVED, 5,
                "디지털기기", "애플매니아", List.of("url1"),
                LocalDateTime.of(2024, 1, 1, 0, 0)
        );
        given(productService.updateProduct(anyLong(), any(ProductUpdateRequest.class), anyLong()))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/products/{productId}", 1L)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("RESERVED"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "product/update-product",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Product")
                                .summary("상품 수정")
                                .description("상품 정보를 수정합니다. 판매자 본인만 가능합니다. null 전송 시 해당 필드는 변경되지 않습니다.")
                                .requestHeaders(headerWithName("Authorization").description("Bearer JWT 액세스 토큰"))
                                .pathParameters(parameterWithName("productId").description("상품 ID"))
                                .requestSchema(Schema.schema("ProductUpdateRequest"))
                                .responseSchema(Schema.schema("ProductResponse"))
                                .requestFields(
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("수정할 제목 (2~40자, null 시 유지)").optional(),
                                        fieldWithPath("description").type(JsonFieldType.STRING).description("수정할 설명 (null 시 유지)").optional(),
                                        fieldWithPath("price").type(JsonFieldType.NUMBER).description("수정할 가격 (100원 이상, null 시 유지)").optional(),
                                        fieldWithPath("categoryId").type(JsonFieldType.NUMBER).description("수정할 카테고리 ID (null 시 유지)").optional(),
                                        fieldWithPath("status").type(JsonFieldType.STRING).description("수정할 상태 (null 시 유지)").optional(),
                                        fieldWithPath("imageUrls").type(JsonFieldType.ARRAY).description("이미지 URL 목록 (null 시 유지, 전달 시 전체 교체)").optional()
                                )
                                .responseFields(productResponseFields())
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("상품 삭제 - DELETE /products/{productId}")
    @WithMockCustomUser
    void deleteProduct() throws Exception {
        // given
        doNothing().when(productService).deleteProduct(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/products/{productId}", 1L)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "product/delete-product",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Product")
                                .summary("상품 삭제")
                                .description("상품을 삭제합니다. 판매자 본인만 가능합니다.")
                                .requestHeaders(headerWithName("Authorization").description("Bearer JWT 액세스 토큰"))
                                .pathParameters(parameterWithName("productId").description("상품 ID"))
                                .responseSchema(Schema.schema("DeleteProductResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional()
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("내 상품 목록 조회 - GET /products/me")
    @WithMockCustomUser
    void getMyProducts() throws Exception {
        // given
        CursorPageResponse<ProductListResponse> response = new CursorPageResponse<>(
                List.of(new ProductListResponse(1L, "내가 파는 상품", 50000, ProductEnum.SALE,
                        "https://img.url/thumb.jpg", 0, LocalDateTime.of(2024, 1, 1, 0, 0))),
                null, false
        );
        given(productService.getMyProducts(anyLong(), any(), any(), any(Pageable.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/products/me")
                        .header("Authorization", "Bearer test-token")
                        .param("status", "SALE")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].title").value("내가 파는 상품"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "product/get-my-products",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Product")
                                .summary("내 상품 목록 조회")
                                .description("로그인한 회원이 등록한 상품 목록을 커서 기반 페이지네이션으로 조회합니다.")
                                .requestHeaders(headerWithName("Authorization").description("Bearer JWT 액세스 토큰"))
                                .queryParameters(
                                        parameterWithName("status").description("상태 필터 (SALE / RESERVED / SOLD, 미전달 시 전체)").optional(),
                                        parameterWithName("cursor").description("페이지 커서 (첫 페이지는 생략)").optional(),
                                        parameterWithName("size").description("페이지 크기 (기본값 10)").optional(),
                                        parameterWithName("page").description("페이지 번호").optional(),
                                        parameterWithName("sort").description("정렬 옵션").optional()
                                )
                                .responseSchema(Schema.schema("ProductListPageResponse"))
                                .responseFields(productListPageResponseFields())
                                .build()
                        )
                ));
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] productResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("상품 ID"),
                fieldWithPath("data.sellerId").type(JsonFieldType.NUMBER).description("판매자 ID"),
                fieldWithPath("data.title").type(JsonFieldType.STRING).description("상품 제목"),
                fieldWithPath("data.description").type(JsonFieldType.STRING).description("상품 설명"),
                fieldWithPath("data.price").type(JsonFieldType.NUMBER).description("가격"),
                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상품 상태 (SALE / RESERVED / SOLD)"),
                fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("찜 수"),
                fieldWithPath("data.categoryName").type(JsonFieldType.STRING).description("카테고리명"),
                fieldWithPath("data.sellerNickname").type(JsonFieldType.STRING).description("판매자 닉네임"),
                fieldWithPath("data.imageUrls").type(JsonFieldType.ARRAY).description("상품 이미지 URL 목록"),
                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("등록 일시")
        };
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] productListPageResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("상품 ID"),
                fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("상품 제목"),
                fieldWithPath("data.content[].price").type(JsonFieldType.NUMBER).description("가격"),
                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("상품 상태"),
                fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 URL").optional(),
                fieldWithPath("data.content[].likeCount").type(JsonFieldType.NUMBER).description("찜 수"),
                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("등록 일시"),
                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서 (마지막 페이지면 null)").optional(),
                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
        };
    }
}
