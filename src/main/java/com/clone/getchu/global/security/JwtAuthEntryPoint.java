package com.clone.getchu.global.security;

import com.clone.getchu.global.common.ErrorResponse;
import com.clone.getchu.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * [인증 실패 핸들러] HTTP 401 Unauthorized 응답 담당
 *
 * 로그인 자체가 안 된 상태(토큰 없음, 만료, 서명 불일치)로
 * 인증이 필요한 API에 접근했을 때 Spring Security가 이 클래스를 호출합니다.
 *
 * SecurityConfig에서 .authenticationEntryPoint(jwtAuthEntryPoint) 로 등록됩니다.
 *
 * [ErrorCode 활용]
 * 하드코딩("A003", "인증이 필요합니다.") 대신 ErrorCode.UNAUTHORIZED를 참조해서
 * 에러코드 변경 시 한 곳(ErrorCode)에서만 수정하면 되도록 일관성을 유지합니다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(ErrorCode.UNAUTHORIZED)));
    }
}
