package com.clone.getchu.domain.auth.dto;

/**
 * [로그인 응답 DTO]
 * POST /auth/login → 200 OK
 *
 * - accessToken  : 인증용 JWT (만료 15분)
 * - refreshToken : 재발급용 JWT (만료 7일), Redis에 서버 측 저장
 * - tokenType    : 항상 "Bearer"
 * - expiresIn    : Access Token 유효시간(초)
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    // tokenType은 항상 "Bearer" 고정 → 호출 편의를 위한 팩토리 메서드
    public static LoginResponse of(String accessToken, String refreshToken, long accessExpirationMs) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", accessExpirationMs / 1000);
    }
}
