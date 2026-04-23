package com.clone.getchu.domain.auth.controller;

import com.clone.getchu.domain.auth.dto.LoginRequest;
import com.clone.getchu.domain.auth.dto.LoginResponse;
import com.clone.getchu.domain.auth.dto.RefreshRequest;
import com.clone.getchu.domain.auth.dto.SignupRequest;
import com.clone.getchu.domain.auth.dto.SignupResponse;
import com.clone.getchu.domain.auth.service.AuthService;
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

import java.time.LocalDateTime;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends RestDocsSupport {

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 - POST /auth/signup")
    void signup() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
                "test@email.com", "Test1234!", "테스트유저", null
        );
        SignupResponse response = new SignupResponse(
                1L, "test@email.com", "테스트유저", LocalDateTime.of(2024, 1, 1, 0, 0)
        );
        given(authService.signup(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.email").value("test@email.com"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "auth/signup",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("회원가입")
                                .description("이메일, 비밀번호, 닉네임으로 회원가입합니다. 이메일 중복 시 409를 반환합니다.")
                                .requestSchema(Schema.schema("SignupRequest"))
                                .responseSchema(Schema.schema("SignupResponse"))
                                .requestFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 (이메일 형식 필수)"),
                                        fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호 (8자 이상, 영문·숫자·특수문자 각 1자 이상)"),
                                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임 (2~20자)"),
                                        fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL (선택)").optional()
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 일시")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("로그인 - POST /auth/login")
    void login() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "Test1234!");
        LoginResponse response = LoginResponse.of(
                "eyJhbGciOiJIUzI1NiJ9.access",
                "eyJhbGciOiJIUzI1NiJ9.refresh",
                900_000L
        );
        given(authService.login(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "auth/login",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("로그인")
                                .description("이메일과 비밀번호로 로그인합니다. 성공 시 Access Token과 Refresh Token을 반환합니다.")
                                .requestSchema(Schema.schema("LoginRequest"))
                                .responseSchema(Schema.schema("LoginResponse"))
                                .requestFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("Access Token (JWT)"),
                                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("Refresh Token (JWT)"),
                                        fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer 고정)"),
                                        fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER).description("Access Token 유효시간 (초)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("토큰 재발급 - POST /auth/refresh")
    void refresh() throws Exception {
        // given
        RefreshRequest request = new RefreshRequest("eyJhbGciOiJIUzI1NiJ9.refresh");
        LoginResponse response = LoginResponse.of(
                "eyJhbGciOiJIUzI1NiJ9.new-access",
                "eyJhbGciOiJIUzI1NiJ9.new-refresh",
                900_000L
        );
        given(authService.refresh(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "auth/refresh",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("토큰 재발급")
                                .description("만료된 Access Token을 Refresh Token으로 재발급합니다. RT Rotation 방식으로 RT도 함께 갱신됩니다.")
                                .requestSchema(Schema.schema("RefreshRequest"))
                                .responseSchema(Schema.schema("LoginResponse"))
                                .requestFields(
                                        fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("Refresh Token")
                                )
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새 Access Token"),
                                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("새 Refresh Token"),
                                        fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer 고정)"),
                                        fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER).description("Access Token 유효시간 (초)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("로그아웃 - POST /auth/logout")
    @WithMockCustomUser
    void logout() throws Exception {
        // given
        doNothing().when(authService).logout(any(), any());

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "auth/logout",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("로그아웃")
                                .description("Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다.")
                                .requestHeaders(
                                        headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                                )
                                .responseSchema(Schema.schema("LogoutResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("로그아웃 되었습니다.").optional()
                                )
                                .build()
                        )
                ));
    }
}
