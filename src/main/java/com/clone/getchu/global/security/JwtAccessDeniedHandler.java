package com.clone.getchu.global.security;

import com.clone.getchu.global.common.ErrorResponse;
import com.clone.getchu.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * [인가 실패 핸들러] HTTP 403 Forbidden 응답 담당
 *
 * 토큰은 있지만 해당 API에 대한 권한이 없을 때 Spring Security가 이 클래스를 호출합니다.
 * 예) USER 권한으로 ADMIN 전용 API 접근 시
 *
 * SecurityConfig에서 .accessDeniedHandler(jwtAccessDeniedHandler) 로 등록됩니다.
 *
 * [JwtAuthEntryPoint와 차이]
 * - JwtAuthEntryPoint      → 401 (인증 실패 - 토큰 없음/만료/서명 오류)
 * - JwtAccessDeniedHandler → 403 (인가 실패 - 토큰은 있지만 권한 없음)
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus().value());
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(ErrorCode.FORBIDDEN)));
    }
}
