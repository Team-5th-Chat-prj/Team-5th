package com.clone.getchu.domain.auth.controller;

import com.clone.getchu.domain.auth.dto.LoginRequest;
import com.clone.getchu.domain.auth.dto.LoginResponse;
import com.clone.getchu.domain.auth.dto.RefreshRequest;
import com.clone.getchu.domain.auth.dto.SignupRequest;
import com.clone.getchu.domain.auth.dto.SignupResponse;
import com.clone.getchu.domain.auth.service.AuthService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.UnauthorizedException;
import com.clone.getchu.global.security.CustomUserDetails;
import com.clone.getchu.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입
     * POST /auth/signup
     * <p>
     * 성공: 201 Created + SignupResponse (id, email, nickname)
     * 실패: 409 DUPLICATE_EMAIL  (이메일 중복)
     * 400 INVALID_REQUEST  (@Valid 검증 실패)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 로그인
     * POST /auth/login
     * <p>
     * 성공: 200 OK + LoginResponse (accessToken, refreshToken, tokenType, expiresIn)
     * 실패: 404 MEMBER_NOT_FOUND    (존재하지 않는 이메일)
     * 401 INVALID_CREDENTIALS (비밀번호 불일치)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 토큰 재발급 (RT Rotation)
     * POST /auth/refresh
     * <p>
     * AT가 만료된 상황에서 호출 → Authorization 헤더 대신 Body로 RT를 전달
     * 성공: 200 OK + LoginResponse (새 AT + 새 RT)
     * 실패: 401 TOKEN_EXPIRED  (RT 만료 — 재로그인 필요)
     * 401 TOKEN_INVALID  (RT 위변조 or 이미 로그아웃된 상태)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 로그아웃
     * POST /auth/logout (🔒 인증 필요)
     * <p>
     * 1. AT를 Redis 블랙리스트에 등록 (남은 만료 시간만큼 TTL)
     * 2. Redis에서 RT 삭제 → 이후 재발급 불가
     * <p>
     * 성공: 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        // SecurityContext에 저장된 인증 정보를 통해 안전하게 memberId를 전달합니다.
        // 클라이언트가 보낸 원본 토큰 문자열은 request에서 추출합니다.
        String accessToken = jwtProvider.resolveToken(request);
        if (accessToken == null) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }
        authService.logout(userDetails.getMemberId(), accessToken);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
    }
}

