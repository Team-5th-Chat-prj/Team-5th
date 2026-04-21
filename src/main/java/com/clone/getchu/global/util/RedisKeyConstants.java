package com.clone.getchu.global.util;
/**
 * Redis 키 접두사 상수 및 키 생성 메서드 관리
 *
 * [키 설계]
 * - RT:{memberId}    : Refresh Token 저장 (TTL = 7일)
 * - BL:{accessToken} : 로그아웃된 AT 블랙리스트 (TTL = AT 남은 만료시간)
 *
 * [키 생성 메서드를 두는 이유]
 * - 호출부에서 PREFIX + 값 연결 코드 제거 → 가독성 향상
 * - 키 형식 변경 시 이 클래스만 수정하면 됨 → 유지보수 용이
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
        throw new IllegalStateException("Utility class");
    }

    // Refresh Token: "RT:{memberId}"
    private static final String RT_PREFIX = "RT:";

    // Access Token 블랙리스트: "BL:{accessToken}"
    private static final String BL_PREFIX = "BL:";

    // 닉네임 선점 락: "LOCK:NICKNAME:{nickname}"
    private static final String NICKNAME_LOCK_PREFIX = "LOCK:NICKNAME:";
    /**
     * Refresh Token Redis 키 생성
     * 예: refreshTokenKey(1L) → "RT:1"
     */
    public static String refreshTokenKey(Long memberId) {
        return RT_PREFIX + memberId;
    }

    /**
     * Access Token 블랙리스트 Redis 키 생성
     * 예: blacklistKey("eyJ...") → "BL:eyJ..."
     */
    public static String blacklistKey(String accessToken) {
        return BL_PREFIX + accessToken;
    }

    /**
     * 닉네임 선점 락 Redis 키 생성
     * 예: nicknameLockKey("홍길동") → "LOCK:NICKNAME:홍길동"
     */
    public static String nicknameLockKey(String nickname) {
        return NICKNAME_LOCK_PREFIX + nickname;
    }
}
