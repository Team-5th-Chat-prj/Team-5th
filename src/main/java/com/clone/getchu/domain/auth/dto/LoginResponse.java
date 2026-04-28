package com.clone.getchu.domain.auth.dto;

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
