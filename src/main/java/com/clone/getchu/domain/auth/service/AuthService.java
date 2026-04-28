package com.clone.getchu.domain.auth.service;

import com.clone.getchu.domain.auth.dto.LoginRequest;
import com.clone.getchu.domain.auth.dto.LoginResponse;
import com.clone.getchu.domain.auth.dto.RefreshRequest;
import com.clone.getchu.domain.auth.dto.SignupRequest;
import com.clone.getchu.domain.auth.dto.SignupResponse;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.domain.member.service.MemberService;
import com.clone.getchu.global.exception.ConflictException;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.NotFoundException;
import com.clone.getchu.global.exception.UnauthorizedException;
import com.clone.getchu.global.security.JwtProvider;
import com.clone.getchu.global.util.RedisKeyConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ConflictException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 닉네임 중복 체크 + Lettuce SETNX 락 (공통 검증 위임)
        memberService.validateNicknameAvailable(request.nickname(), null);

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .profileImageUrl(request.profileImageUrl())
                .build();

        try {
            return SignupResponse.from(memberRepository.save(member));
        } catch (DataIntegrityViolationException e) {
            // 추가 DB 조회 없이 예외 메시지로 위반된 제약조건을 판별

            String msg = e.getMostSpecificCause().getMessage();
            if (msg != null && msg.contains("uk_member_nickname")) {
                throw new ConflictException(ErrorCode.DUPLICATE_NICKNAME);
            }
            throw new ConflictException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

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
        // @Value로 주입받던 방식 -> JwtProvider에서 가져오는 방식
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
            // [보안] 저장된 토큰과 불일치 → 토큰 탈취 가능성 → 즉시 로그아웃 처리
            if (storedToken != null) {
                stringRedisTemplate.delete(rtKey);
            }
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
     * → JwtAuthFilter에서 매 요청마다 블랙리스트 조회해 차단
     * 2. Redis에서 RT 삭제 → 이후 재발급 요청 불가
     * <p>
     * [참고] AT 유효성 검증 및 남은 만료 시간 계산은 JwtAuthFilter에서 선행 처리됨.
     * 서비스 계층은 필터가 request 속성("jwt.token", "jwt.remaining")으로 전달한 값을 그대로 사용해
     * 조작·만료 토큰에 대한 재파싱 예외 위험을 제거함.
     * [참고] AT 만료 후 블랙리스트 키가 자동 삭제되므로 Redis 공간 낭비 없음
     * [참고] DB 작업 없이 Redis만 사용하므로 @Transactional 미적용
     */
    public void logout(Long memberId, HttpServletRequest request) {
        // JwtAuthFilter가 검증 후 저장한 속성에서 토큰과 남은 만료 시간을 읽음
        String accessToken = (String) request.getAttribute("jwt.token");
        Long remaining = (Long) request.getAttribute("jwt.remaining");

        if (accessToken == null || remaining == null) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        if (remaining > 0) {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.blacklistKey(accessToken),
                    "logout",
                    remaining,
                    TimeUnit.MILLISECONDS
            );
        }
        stringRedisTemplate.delete(RedisKeyConstants.refreshTokenKey(memberId));
    }
}
