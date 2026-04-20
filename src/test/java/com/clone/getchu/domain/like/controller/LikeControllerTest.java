package com.clone.getchu.domain.like.controller;

import com.clone.getchu.domain.like.service.LikeFacade;
import com.clone.getchu.domain.like.service.LikeService;
import com.clone.getchu.domain.product.dto.ProductListResponse;
import com.clone.getchu.domain.product.entity.ProductEnum;
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
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
class LikeControllerTest extends RestDocsSupport {

    @MockBean
    private LikeService likeService;

    @MockBean
    private LikeFacade likeFacade;

    @Test
    @DisplayName("찜하기 - POST /products/{productId}/likes")
    @WithMockCustomUser
    void createLike() throws Exception {
        // given
        Long productId = 1L;
        doNothing().when(likeFacade).createLike(anyLong(), anyLong());

        // when & then
        mockMvc.perform(post("/products/{productId}/likes", productId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "like/create-like",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Like")
                                .summary("상품 찜하기")
                                .description("특정 상품을 찜 목록에 추가합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .pathParameters(
                                        parameterWithName("productId").description("상품 ID")
                                )
                                .responseSchema(Schema.schema("CreateLikeResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional()
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("찜 취소 - DELETE /products/{productId}/likes")
    @WithMockCustomUser
    void deleteLike() throws Exception {
        // given
        Long productId = 1L;
        doNothing().when(likeFacade).deleteLike(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/products/{productId}/likes", productId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "like/delete-like",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Like")
                                .summary("상품 찜 취소")
                                .description("특정 상품을 찜 목록에서 제거합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .pathParameters(
                                        parameterWithName("productId").description("상품 ID")
                                )
                                .responseSchema(Schema.schema("DeleteLikeResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional()
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("내 찜 목록 조회 - GET /members/me/likes")
    @WithMockCustomUser
    void myLikes() throws Exception {
        // given
        ProductListResponse product = new ProductListResponse(
                1L,
                "아이폰 17",
                1000000,
                ProductEnum.SALE,
                "https://image.url/test.jpg",
                LocalDateTime.now()
        );
        Page<ProductListResponse> pageResponse = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);

        given(likeService.getMyLikesList(anyLong(), any())).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/members/me/likes")
                .header("Authorization", "Bearer test-token")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].id").value(product.id()))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "like/my-likes",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Like")
                                .summary("내 찜 목록 조회")
                                .description("사용자가 찜한 상품 목록을 페이징하여 조회합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .queryParameters(
                                        parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                        parameterWithName("size").description("페이지 크기 (기본값 10)").optional(),
                                        parameterWithName("sort").description("정렬 옵션 (기본값 createdAt,desc)").optional()
                                )
                                .responseSchema(Schema.schema("MyLikesResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("상품 ID"),
                                        fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("상품 제목"),
                                        fieldWithPath("data.content[].price").type(JsonFieldType.NUMBER).description("상품 가격"),
                                        fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("상품 상태"),
                                        fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING).description("상품 썸네일 URL").optional(),
                                        fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("상품 등록일시"),
                                        fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                        fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                        fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보가 비어있는지 여부"),
                                        fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬되었는지 여부"),
                                        fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않았는지 여부"),
                                        fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("현재 페이지의 시작 오프셋"),
                                        fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이징 여부"),
                                        fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("비페이징 여부"),
                                        fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                        fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                        fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                        fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                        fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 당 요소 수"),
                                        fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                        fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보가 비어있는지 여부"),
                                        fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬되었는지 여부"),
                                        fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않았는지 여부"),
                                        fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지의 실제 요소 수"),
                                        fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("데이터가 비어있는지 여부")
                                )
                                .build()
                        )
                ));
    }
}
