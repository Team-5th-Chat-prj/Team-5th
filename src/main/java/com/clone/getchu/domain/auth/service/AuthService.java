package com.clone.getchu.domain.auth.service;

import com.clone.getchu.domain.auth.dto.LoginRequest;
import com.clone.getchu.domain.auth.dto.LoginResponse;
import com.clone.getchu.domain.auth.dto.RefreshRequest;
import com.clone.getchu.domain.auth.dto.SignupRequest;
import com.clone.getchu.domain.auth.dto.SignupResponse;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.ConflictException;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.NotFoundException;
import com.clone.getchu.global.exception.UnauthorizedException;
import com.clone.getchu.global.security.JwtProvider;
import com.clone.getchu.global.util.RedisKeyConstants;
import com.clone.getchu.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 회원가입
     * 1. 이메일 중복 체크 → 409 DUPLICATE_EMAIL
     * 2. 비밀번호 BCrypt 인코딩
     * 3. Member 엔티티 생성 후 저장
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ConflictException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        return SignupResponse.from(memberRepository.save(member));
    }

    /**
     * 로그인
     * 1. 이메일로 회원 조회 → 없으면 404 MEMBER_NOT_FOUND
     * 2. 비밀번호 BCrypt 검증 → 불일치 시 401 INVALID_CREDENTIALS
     * 3. AT + RT 발급
     * 4. RT를 Redis에 저장 (TTL = 7일)
     *    - 키: "RT:{memberId}" / 기기 1대 기준 → 재로그인 시 이전 RT 자동 덮어쓰기
     *
     * [보안 참고] 이메일 존재 여부를 404로 노출하고 있음.
     * 이메일 열거 공격 방어가 필요하다면 401로 통일 가능.
     * 현재는 API 명세(06-API 명세서.md)에 따라 구분 응답을 유지함.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getNickname(), member.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        // RT를 Redis에 저장 — 서버가 RT 유효성을 직접 제어하기 위함
        String rtKey = RedisKeyConstants.refreshTokenKey(member.getId());
        stringRedisTemplate.opsForValue().set(
                rtKey,
                refreshToken,
                jwtProvider.getRefreshExpirationTime(),
                TimeUnit.MILLISECONDS
        );

        return LoginResponse.of(accessToken, refreshToken, jwtProvider.getAccessExpirationTime());
    }

    /**
     * 토큰 재발급 (RT Rotation)
     * 1. RT 서명·만료 검증 → 실패 시 401
     * 2. Redis에 저장된 RT와 일치 여부 확인 → 불일치 시 401 (이미 로그아웃 or 재사용 공격)
     * 3. memberId로 회원 조회 → DB에서 최신 정보 반영
     * 4. 새 AT + 새 RT 발급 (RT Rotation: 기존 RT 폐기 → 탈취된 RT 재사용 방지)
     * 5. Redis에 새 RT 저장
     */
    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        String oldRefreshToken = request.refreshToken();

        // 1. RT 서명·만료 검증
        jwtProvider.validateRefreshToken(oldRefreshToken);

        // 2. memberId 추출 후 Redis 저장값과 비교
        Long memberId = jwtProvider.getMemberIdFromToken(oldRefreshToken);
        String rtKey = RedisKeyConstants.refreshTokenKey(memberId);
        String storedToken = stringRedisTemplate.opsForValue().get(rtKey);

        if (storedToken == null || !storedToken.equals(oldRefreshToken)) {
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID);
        }

        // 3. DB에서 최신 회원 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        // 4. 새 AT + 새 RT 발급
        String newAccessToken = jwtProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getNickname(), member.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken(member.getId());

        // 5. Redis에 새 RT 저장 (기존 RT 덮어쓰기 → 기존 RT 사용 불가)
        stringRedisTemplate.opsForValue().set(
                rtKey,
                newRefreshToken,
                jwtProvider.getRefreshExpirationTime(),
                TimeUnit.MILLISECONDS
        );

        return LoginResponse.of(newAccessToken, newRefreshToken, jwtProvider.getAccessExpirationTime());
    }
    /**
     * 로그아웃
     * 1. AT를 Redis 블랙리스트에 등록 (남은 만료 시간만큼 TTL 설정)
     *    → JwtAuthFilter에서 매 요청마다 블랙리스트 조회해 차단
     * 2. Redis에서 RT 삭제 → 이후 재발급 요청 불가
     *
     * [참고] AT 만료(15분) 후에는 블랙리스트 키가 자동 삭제되므로 Redis 공간 낭비 없음
     * [참고] DB 작업 없이 Redis만 사용하므로 @Transactional 미적용
     */
    public void logout(String accessToken) {
        // 1. AT 블랙리스트 등록
        long remaining = jwtProvider.getRemainingExpiration(accessToken);
        if (remaining > 0) {
            String blKey = RedisKeyConstants.blacklistKey(accessToken);
            stringRedisTemplate.opsForValue().set(
                    blKey,
                    "logout",
                    remaining,
                    TimeUnit.MILLISECONDS
            );
        }

        // 2. RT 삭제
        Long memberId = SecurityUtil.getCurrentMemberId();
        stringRedisTemplate.delete(RedisKeyConstants.refreshTokenKey(memberId));
    }
}
