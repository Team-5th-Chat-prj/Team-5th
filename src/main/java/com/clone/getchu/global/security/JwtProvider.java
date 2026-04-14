package com.clone.getchu.global.security;

import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * [JWT 핵심 유틸리티] 토큰 생성 / 파싱 / 검증 담당
 * <p>
 * Access Token  : 15분 만료, 인증 클레임(memberId·email·nickname·role) 포함
 * Refresh Token : 7일 만료, subject(memberId)만 포함 — Redis에 저장해 서버가 직접 관리
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessExpirationTime;
    private final long refreshExpirationTime;

    public JwtProvider(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.access-expiration-time}") long accessExpirationTime,
            @Value("${jwt.refresh-expiration-time}") long refreshExpirationTime) {
        // 플랫폼 기본 인코딩 대신 UTF-8을 명시해서 환경(OS/JVM)에 상관없이 동일한 키를 생성
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationTime = accessExpirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
    }

    public long getAccessExpirationTime() {
        return accessExpirationTime;
    }

    public long getRefreshExpirationTime() {
        return refreshExpirationTime;
    }

    // 로그인 성공 시 호출 - memberId, email, role을 JWT 클레임에 담아 토큰 생성
    public String createAccessToken(Long memberId, String email, String nickname, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpirationTime);

        return Jwts.builder()
                .subject(email)
                .claim("memberId", memberId)
                .claim("nickname", nickname) // 채팅방 등에서 DB 조회 없이 닉네임 표시용
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key) // 알고리즘 자동 감지 → 알고리즘 혼동 공격 방지
                .compact();
    }

    /**
     * Refresh Token 생성
     * AT와 달리 subject(memberId)만 담음 — 인증 클레임 불필요, 만료 시간만 길게 설정
     * 실제 유효성은 Redis 저장 여부로 추가 검증 (validateRefreshToken 참고)
     */
    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpirationTime);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token에서 memberId 추출
     * RT는 subject에 memberId만 저장하므로 파싱 후 Long 변환
     */
    public Long getMemberIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 토큰의 남은 유효시간(ms) 반환
     * AT 블랙리스트 등록 시 Redis TTL 계산에 사용
     */
    public long getRemainingExpiration(String token) {
        long expiry = parseClaims(token).getExpiration().getTime();
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    /**
     * Refresh Token 유효성 검증 — 실패 시 예외를 직접 던짐
     * validateToken()과 달리 호출부에서 분기 처리 없이 바로 쓸 수 있음
     */
    public void validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * Access Token 유효성 검증 — 실패 시 예외를 직접 던짐 (JwtAuthFilter 전용)
     * validateToken()과 달리 예외를 호출부로 전파해서 정확한 에러 코드 반환을 가능하게 함
     */
    public void validateTokenOrThrow(String token) {
        Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    // 유효한 토큰에서 인증 객체 생성 - JwtAuthFilter, StompChannelInterceptor에서 호출
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long memberId = claims.get("memberId", Long.class);
        String email = claims.getSubject();
        String nickname = claims.get("nickname", String.class);
        String role = claims.get("role", String.class);

        // DB 조회 없이 클레임만으로 CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(memberId, email, nickname, role);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * 토큰의 서명, 만료 여부 등을 검증
     * true면 유효, false면 무효
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("유효하지않은 JWT 서명입니다. - {}", ErrorCode.TOKEN_INVALID.getMessage(), e);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다. - {}", ErrorCode.TOKEN_EXPIRED.getMessage(), e);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다. - {}", ErrorCode.TOKEN_INVALID.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims가 비어있습니다. - {}", ErrorCode.TOKEN_INVALID.getMessage(), e);
        }
        return false;
    }

    /**
     * 토큰에서 클레임 추출
     * 만료된 토큰이라도 클레임 자체는 읽을 수 있어서 ExpiredJwtException 별도 처리
     * validateToken()에서 이미 검증 통과한 토큰만 여기 도달하므로 다른 예외는 사실상 발생 안 함
     */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료됐어도 클레임은 읽을 수 있음(방어적 코드)
            return e.getClaims();
        }
    }
}
