package com.clone.getchu.domain.member.service;

import com.clone.getchu.domain.member.dto.request.MemberUpdateRequest;
import com.clone.getchu.domain.member.dto.request.UpdatePasswordRequest;
import com.clone.getchu.domain.member.dto.response.MemberProfileResponse;
import com.clone.getchu.domain.member.dto.response.MemberResponse;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.InvalidRequestException;
import com.clone.getchu.global.exception.NotFoundException;
import com.clone.getchu.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

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
        member.update(request.nickname(), request.profileImageUrl());
        return MemberResponse.from(member);
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
