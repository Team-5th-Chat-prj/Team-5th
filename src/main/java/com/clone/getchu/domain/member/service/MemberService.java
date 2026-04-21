package com.clone.getchu.domain.member.service;

import com.clone.getchu.domain.member.dto.request.MemberUpdateRequest;
import com.clone.getchu.domain.member.dto.request.UpdatePasswordRequest;
import com.clone.getchu.domain.member.dto.response.MemberProfileResponse;
import com.clone.getchu.domain.member.dto.response.MemberResponse;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.ConflictException;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.InvalidRequestException;
import com.clone.getchu.global.exception.NotFoundException;
import com.clone.getchu.global.security.CustomUserDetails;
import com.clone.getchu.global.util.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    // 내 UUID와 일치할 때만 삭제 — TTL 초과 후 다른 스레드가 획득한 락을 실수로 해제하는 것을 방지
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    // 내 정보 조회(로그인 필요)
    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    // 회원 정보 수정 (닉네임, 프로필 이미지)
    @Transactional
    public MemberResponse update(CustomUserDetails userDetails, MemberUpdateRequest request) {
        Member member = memberRepository.findById(userDetails.getMemberId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        validateNicknameAvailable(request.nickname(), member.getNickname());

        member.update(request.nickname(), request.profileImageUrl());
        return MemberResponse.from(member);
    }

    /**
     * 닉네임 사용 가능 여부 검증 (Lettuce SETNX 락 + DB 중복 체크)
     *
     * - nickname이 null이거나 현재 닉네임과 동일하면 검증 생략
     * - SETNX 실패: 다른 스레드가 동일 닉네임을 처리 중 → 409
     * - 락 값에 UUID 사용: TTL 초과 후 다른 스레드가 재획득한 락을 실수로 해제하는 것을 방지
     * - Lua 스크립트로 "UUID 확인 + 삭제"를 원자적으로 처리
     * - 락은 check 범위만 보호. save 이후는 DB unique constraint가 최종 방어선.
     */
    public void validateNicknameAvailable(String nickname, String currentNickname) {
        if (nickname == null || nickname.equals(currentNickname)) return;

        String lockKey = RedisKeyConstants.nicknameLockKey(nickname);
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new ConflictException(ErrorCode.DUPLICATE_NICKNAME);
        }
        try {
            if (memberRepository.existsByNickname(nickname)) {
                throw new ConflictException(ErrorCode.DUPLICATE_NICKNAME);
            }
        } finally {
            // 내 UUID와 일치할 때만 삭제 (원자적 실행)
            stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
        }
    }

    // 회원 탈퇴 (소프트 삭제)
    @Transactional
    public void delete(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        member.delete();
    }

    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long memberId, UpdatePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        // 현재 비밀번호 일치 여부 검증
        if (!passwordEncoder.matches(request.oldPassword(), member.getPassword())) {
            throw new InvalidRequestException(ErrorCode.INVALID_PASSWORD);
        }

        // 새 비밀번호가 현재 비밀번호와 동일하면 차단
        // → 변경 의도 없는 요청을 허용하면 불필요한 해시 연산 + 보안 감사 로그 오염
        if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
            throw new InvalidRequestException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        member.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    // 타인 정보 조회(비로그인 상태도가능)
    public MemberProfileResponse getMemberProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberProfileResponse.from(member);
    }
}
