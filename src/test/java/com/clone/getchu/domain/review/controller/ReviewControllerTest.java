package com.clone.getchu.domain.review.controller;

import com.clone.getchu.domain.review.dto.request.ReviewRequest;
import com.clone.getchu.domain.review.dto.response.ReviewResponse;
import com.clone.getchu.domain.review.service.ReviewService;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.security.*;
import com.clone.getchu.support.RestDocsSupport;
import com.clone.getchu.support.WithMockCustomUser;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest extends RestDocsSupport {

    // ── Redis/Redisson: 실제 연결 없이 컨텍스트 로딩
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;
    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private ReviewService reviewService;

    // SecurityConfig 의존성 해소
    @MockBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;
    @MockBean
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    // ── 픽스처 ────────────────────────────────────────────────────────────────

    private ReviewRequest reviewRequest() {
        return new ReviewRequest(new BigDecimal("4.5"), "친절하고 물건도 좋았습니다. 다음에 또 거래하고 싶어요.");
    }

    private List<ReviewResponse> reviewList() {
        return List.of(
                new ReviewResponse(
                        2L,
                        new BigDecimal("4.5"),
                        "친절하고 물건도 좋았습니다.",
                        LocalDateTime.of(2024, 6, 10, 14, 0),
                        "구매자닉네임",
                        "https://cdn.example.com/buyer.jpg",
                        "아이폰 15 팝니다"
                ),
                new ReviewResponse(
                        1L,
                        new BigDecimal("3.5"),
                        "무난한 거래였습니다.",
                        LocalDateTime.of(2024, 5, 20, 9, 30),
                        "다른구매자",
                        null,
                        "맥북 에어 M2"
                )
        );
    }

    // ── 테스트 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("리뷰 작성 - POST /trades/{tradeId}/reviews")
    @WithMockCustomUser(memberId = 1L, email = "test@email.com", nickname = "테스트유저")
    void createReview() throws Exception {
        doNothing().when(reviewService).createReview(any(), anyLong(), any(ReviewRequest.class));

        mockMvc.perform(post("/trades/{tradeId}/reviews", 1L)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "review/create-review",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Review")
                                .summary("리뷰 작성")
                                .description("""
                                        거래 완료(SOLD) 상태인 거래에 대해 구매자가 판매자에게 리뷰를 작성합니다.
                                        - 거래당 리뷰는 1건만 작성 가능합니다.
                                        - 평점은 0.5 ~ 5.0 사이의 0.5 단위 값만 허용됩니다.
                                        """)
                                .requestSchema(Schema.schema("ReviewRequest"))
                                .responseSchema(Schema.schema("ReviewCreateResponse"))
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .pathParameters(
                                        parameterWithName("tradeId").description("리뷰를 작성할 거래 ID")
                                )
                                .requestFields(
                                        fieldWithPath("rating").type(JsonFieldType.NUMBER)
                                                .description("평점 (0.5 ~ 5.0, 0.5 단위)"),
                                        fieldWithPath("content").type(JsonFieldType.STRING)
                                                .description("리뷰 내용 (500자 이내)")
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("리뷰 목록 조회 - GET /members/{memberId}/reviews")
    @WithAnonymousUser
    void getReviews() throws Exception {
        CursorPageResponse<ReviewResponse> cursorResponse = new CursorPageResponse<>(
                reviewList(),
                "eyJpZCI6MX0=",  // base64("1")
                true
        );
        given(reviewService.getReviews(anyLong(), any(), anyInt())).willReturn(cursorResponse);

        mockMvc.perform(get("/members/{memberId}/reviews", 1L)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "review/get-reviews",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Review")
                                .summary("리뷰 목록 조회")
                                .description("""
                                        특정 회원이 받은 리뷰 목록을 커서 기반 페이지네이션으로 조회합니다.
                                        - 비로그인 상태에서도 조회 가능합니다.
                                        - 최신 리뷰가 먼저 반환됩니다.
                                        - 다음 페이지 조회 시 응답의 nextCursor 값을 cursor 파라미터로 전달합니다.
                                        """)
                                .responseSchema(Schema.schema("ReviewListResponse"))
                                .pathParameters(
                                        parameterWithName("memberId").description("리뷰를 조회할 회원 ID")
                                )
                                .queryParameters(
                                        parameterWithName("cursor").description("페이지 커서 (첫 페이지는 생략)").optional(),
                                        parameterWithName("size").description("페이지 크기 (기본값: 10)").optional()
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                        fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("리뷰 ID"),
                                        fieldWithPath("data.content[].rating").type(JsonFieldType.NUMBER).description("평점 (0.5 ~ 5.0)"),
                                        fieldWithPath("data.content[].content").type(JsonFieldType.STRING).description("리뷰 내용"),
                                        fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("작성 일시"),
                                        fieldWithPath("data.content[].reviewerNickname").type(JsonFieldType.STRING).description("리뷰 작성자 닉네임"),
                                        fieldWithPath("data.content[].reviewerProfileImageUrl").type(JsonFieldType.STRING).description("리뷰 작성자 프로필 이미지 URL (없으면 null)").optional(),
                                        fieldWithPath("data.content[].productTitle").type(JsonFieldType.STRING).description("거래된 상품명"),
                                        fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서 (마지막 페이지면 null)").optional(),
                                        fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                                )
                                .build()
                        )
                ));
    }
}
