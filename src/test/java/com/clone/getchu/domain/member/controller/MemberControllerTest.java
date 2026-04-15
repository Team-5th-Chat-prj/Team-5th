package com.clone.getchu.domain.member.controller;

import com.clone.getchu.domain.auth.service.AuthService;
import com.clone.getchu.domain.member.dto.request.MemberUpdateRequest;
import com.clone.getchu.domain.member.dto.request.UpdatePasswordRequest;
import com.clone.getchu.domain.member.dto.response.MemberProfileResponse;
import com.clone.getchu.domain.member.dto.response.MemberResponse;
import com.clone.getchu.domain.member.service.MemberService;
import com.clone.getchu.global.security.*;
import com.clone.getchu.support.RestDocsSupport;
import com.clone.getchu.support.WithMockCustomUser;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
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

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest extends RestDocsSupport {

    // ── Redis/Redisson: 실제 연결 없이 컨텍스트 로딩
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;  // RedisConfig.redisTemplate() 의존성 해소
    @MockBean
    private StringRedisTemplate stringRedisTemplate;        // JwtAuthFilter 블랙리스트 체크
    @MockBean
    private RedissonClient redissonClient;                  // Redisson 자동 연결 차단

    @MockBean
    MemberService memberService;

    @MockBean
    AuthService authService;  // ⬅ 추가

    @MockBean
    private JwtAuthFilter jwtAuthFilter;
    @MockBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockBean
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "test@test.com";
    private static final String NICKNAME = "테스터";

    @BeforeEach
    void setUpJwtFilter() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    // ── 공통 응답 픽스처 ──────────────────────────────────────────────────────

    private MemberResponse memberResponse() {
        return new MemberResponse(
                MEMBER_ID, EMAIL, NICKNAME,
                "https://cdn.example.com/profile.jpg",
                BigDecimal.valueOf(4.5), 12,
                LocalDateTime.of(2024, 1, 1, 0, 0)
        );
    }

    // ── 테스트 ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("내 정보 조회 - GET /members/me")
    @WithMockCustomUser(memberId = 1L, email = "test@email.com", nickname = "테스트유저")
    void getMyInfo() throws Exception {
        // given
        given(memberService.getMyInfo(any()))
                .willReturn(new MemberResponse(
                        1L,
                        "test@email.com",
                        "테스트유저",
                        "https://example.com/profile.jpg",
                        new BigDecimal("4.5"),
                        10,
                        LocalDateTime.now()
                ));

        // when & then
        mockMvc.perform(get("/members/me")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentationWrapper.document("member-get-me",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Member")
                                .summary("내 정보 조회")
                                .responseSchema(Schema.schema("MemberResponse"))
                                .requestHeaders(headerWithName("Authorization").description("Bearer JWT 액세스 토큰"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                        fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                        fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                                        fieldWithPath("data.averageRating").type(JsonFieldType.NUMBER).description("평균 평점"),
                                        fieldWithPath("data.reviewCount").type(JsonFieldType.NUMBER).description("리뷰 수"),
                                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 일시")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("내 정보 수정 - PATCH /members/me")
    @WithMockCustomUser(memberId = 1L, email = "test@email.com", nickname = "테스트유저")
    void updateMember() throws Exception {
        MemberUpdateRequest request = new MemberUpdateRequest("새닉네임", "https://cdn.example.com/new.jpg");
        MemberResponse updated = new MemberResponse(
                MEMBER_ID, EMAIL, "새닉네임",
                "https://cdn.example.com/new.jpg",
                BigDecimal.valueOf(4.5), 12,
                LocalDateTime.of(2024, 1, 1, 0, 0)
        );
        when(memberService.update(any(CustomUserDetails.class), any(MemberUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/members/me")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "member/update-member",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Member")
                                        .summary("내 정보 수정")
                                        .responseSchema(Schema.schema("MemberResponse"))
                                        .requestSchema(Schema.schema("MemberUpdateRequest"))
                                        .description("닉네임 또는 프로필 이미지 URL을 수정합니다.\n" +
                                                "변경하지 않을 필드는 null로, 프로필 이미지를 삭제하려면 빈 문자열(\"\")로 전송합니다.")
                                        .requestHeaders(
                                                headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                        )
                                        .requestFields(
                                                fieldWithPath("nickname").type(JsonFieldType.STRING)
                                                        .description("변경할 닉네임 (2~20자). null 전송 시 변경 없음").optional(),
                                                fieldWithPath("profileImageUrl").type(JsonFieldType.STRING)
                                                        .description("변경할 프로필 이미지 URL. \"\" 전송 시 이미지 삭제, null 전송 시 변경 없음").optional()
                                        )
                                        .responseFields(
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                                fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("변경된 닉네임"),
                                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("변경된 프로필 이미지 URL").optional(),
                                                fieldWithPath("data.averageRating").type(JsonFieldType.NUMBER).description("평균 평점"),
                                                fieldWithPath("data.reviewCount").type(JsonFieldType.NUMBER).description("리뷰 수"),
                                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 일시")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("회원 탈퇴 - DELETE /members/me")
    @WithMockCustomUser(memberId = 1L, email = "test@email.com", nickname = "테스트유저")
    void deleteMember() throws Exception {
        doNothing().when(memberService).delete(MEMBER_ID);

        mockMvc.perform(delete("/members/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "member/delete-member",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Member")
                                        .summary("회원 탈퇴")
                                        .responseSchema(Schema.schema("DeleteMemberResponse"))
                                        .description("로그인한 회원 계정을 삭제합니다.")
                                        .requestHeaders(
                                                headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                        )
                                        .responseFields(
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("\"회원탈퇴가 완료되었습니다.\"")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("비밀번호 변경 - PATCH /members/me/password")
    @WithMockCustomUser(memberId = 1L, email = "test@email.com", nickname = "테스트유저")
    void updatePassword() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("OldPass1!", "NewPass2@");
        doNothing().when(memberService).updatePassword(anyLong(), any(UpdatePasswordRequest.class));

        mockMvc.perform(patch("/members/me/password")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "member/update-password",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Member")
                                        .summary("비밀번호 변경")
                                        .requestSchema(Schema.schema("UpdatePasswordRequest"))
                                        .responseSchema(Schema.schema("UpdatePasswordResponse"))
                                        .description("현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.\n" +
                                                "새 비밀번호는 영문·숫자·특수문자를 각각 1자 이상 포함해야 합니다.")
                                        .requestHeaders(
                                                headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                        )
                                        .requestFields(
                                                fieldWithPath("oldPassword").type(JsonFieldType.STRING).description("현재 비밀번호"),
                                                fieldWithPath("newPassword").type(JsonFieldType.STRING)
                                                        .description("새 비밀번호 (8자 이상, 영문·숫자·특수문자 각 1자 이상 포함)")
                                        )
                                        .responseFields(
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("\"비밀번호가 변경되었습니다.\"")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("타 회원 프로필 조회 - GET /members/{memberId}")
    @WithAnonymousUser
    void getMemberProfile() throws Exception {
        MemberProfileResponse profile = new MemberProfileResponse(
                2L, "상대방닉네임", "https://cdn.example.com/other.jpg",
                BigDecimal.valueOf(4.2), 7,
                LocalDateTime.of(2024, 3, 15, 0, 0)
        );
        when(memberService.getMemberProfile(2L)).thenReturn(profile);

        // 비로그인도 가능한 API — Authorization 헤더 없이 호출
        mockMvc.perform(get("/members/{memberId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "member/get-member-profile",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Member")
                                        .summary("타 회원 프로필 조회")
                                        .responseSchema(Schema.schema("MemberProfileResponse"))
                                        .description("비로그인 상태에서도 조회 가능한 타 회원의 공개 프로필 정보를 조회합니다.")
                                        .pathParameters(
                                                parameterWithName("memberId").description("조회할 회원 ID")
                                        )
                                        .responseFields(
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL (없으면 null)").optional(),
                                                fieldWithPath("data.averageRating").type(JsonFieldType.NUMBER).description("평균 평점"),
                                                fieldWithPath("data.reviewCount").type(JsonFieldType.NUMBER).description("리뷰 수"),
                                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 일시")
                                        )
                                        .build()
                        )
                ));
    }
}
