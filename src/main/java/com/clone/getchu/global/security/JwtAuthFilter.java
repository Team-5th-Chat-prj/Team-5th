package com.clone.getchu.global.security;

import com.clone.getchu.global.common.ErrorResponse;
import com.clone.getchu.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.clone.getchu.global.util.RedisKeyConstants;

import java.io.IOException;

/**
 * [JWT 인증 필터] 모든 HTTP 요청에서 토큰을 추출해 인증 처리
 * <p>
 * OncePerRequestFilter를 상속해서 하나의 요청에 딱 한 번만 실행됩니다.
 * SecurityConfig에서 UsernamePasswordAuthenticationFilter 앞에 등록되어
 * 요청이 컨트롤러에 도달하기 전에 JWT를 검사합니다.
 * <p>
 * 처리 흐름:
 * 1. Authorization 헤더에서 Bearer 토큰 추출
 * 2. JWT 유효성 검증
 * 3. Redis 블랙리스트 토큰인지 확인
 * 4. 정상 토큰이면 Authentication 생성 후 SecurityContext 저장
 * 5. 토큰이 없으면 다음 필터로 넘김
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = jwtProvider.resolveToken(request);
        // JWT가 없는 경우:
        // - 인증 정보가 없는 요청으로 처리
        // - SecurityContext를 설정하지 않고 다음 필터로 넘김
        // - 인증이 필요한 API라면 이후 Security가 401 응답 처리
        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            // validateToken() 은 예외를 내부에서 swallow하므로 만료/무효 구분이 불가.
            // validateTokenOrThrow() 로 예외를 전파받아 정확한 에러코드를 반환한다.
            jwtProvider.validateTokenOrThrow(jwt);

            // 로그아웃된 토큰(블랙리스트) 차단 — Redis에 "BL:{token}" 키 존재 여부 확인
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.blacklistKey(jwt)))) {
                log.warn("블랙리스트 처리된 Access Token입니다. code={}, message={}",
                        ErrorCode.LOGGED_OUT_TOKEN.getCode(),
                        ErrorCode.LOGGED_OUT_TOKEN.getMessage());
                writeErrorResponse(response, ErrorCode.LOGGED_OUT_TOKEN);
                return;
            }

            // 남은 만료 시간을 request 속성으로 저장 — logout 시 서비스 계층에서 재파싱 없이 사용
            long remaining = jwtProvider.getRemainingExpiration(jwt);
            request.setAttribute("jwt.token", jwt);
            request.setAttribute("jwt.remaining", remaining);

            Authentication authentication = jwtProvider.getAuthentication(jwt);
            // SecurityContext에 인증 정보 저장 - 이후 요청 처리 동안 어디서든 꺼낼 수 있음
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (SecurityException | MalformedJwtException e) {
            log.warn("유효하지 않은 JWT 서명입니다. code={}, message={}",
                    ErrorCode.TOKEN_INVALID.getCode(),
                    ErrorCode.TOKEN_INVALID.getMessage(),
                    e);
            writeErrorResponse(response, ErrorCode.TOKEN_INVALID);
            return;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다. code={}, message={}",
                    ErrorCode.TOKEN_EXPIRED.getCode(),
                    ErrorCode.TOKEN_EXPIRED.getMessage(),
                    e);
            writeErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다. code={}, message={}",
                    ErrorCode.TOKEN_INVALID.getCode(),
                    ErrorCode.TOKEN_INVALID.getMessage(),
                    e);
            writeErrorResponse(response, ErrorCode.TOKEN_INVALID, "지원되지 않는 JWT 토큰입니다.");
            return;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims가 비어있습니다. code={}, message={}",
                    ErrorCode.TOKEN_INVALID.getCode(),
                    ErrorCode.TOKEN_INVALID.getMessage(),
                    e);
            writeErrorResponse(response, ErrorCode.TOKEN_INVALID, "잘못된 JWT 토큰입니다.");
            return;
        }
        // 현재 코드는 유효하지 않으면 이미 return으로 처리함
        // 여기 도달하는 건 "정상 토큰" 또는 "토큰 없음" 두 경우뿐
        // 정상 처리 완료 후 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        writeErrorResponse(response, ErrorResponse.of(errorCode), errorCode.getHttpStatus().value());
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode, String detailMessage) throws IOException {
        writeErrorResponse(response, ErrorResponse.of(errorCode, detailMessage), errorCode.getHttpStatus().value());
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorResponse errorResponse, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}