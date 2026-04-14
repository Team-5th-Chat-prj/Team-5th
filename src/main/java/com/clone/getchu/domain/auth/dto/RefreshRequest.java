package com.clone.getchu.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [토큰 재발급 요청 DTO]
 * POST /auth/refresh
 *
 * Authorization 헤더 대신 Body로 받는 이유:
 * 재발급 시점에는 AT가 만료된 상태이므로 JwtAuthFilter를 통과할 수 없음.
 * RT를 Body에 담아 서비스 계층에서 직접 검증한다.
 */
public record RefreshRequest(

        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {}
