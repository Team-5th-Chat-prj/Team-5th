package com.clone.getchu.domain.auth.dto;

import com.clone.getchu.domain.member.entity.Member;

import java.time.LocalDateTime;

/**
 * [회원가입 응답 DTO]
 * POST /auth/signup → 201 Created
 *
 * 비밀번호는 응답에서 제외
 */
public record SignupResponse(
        Long id,
        String email,
        String nickname,
        LocalDateTime createdAt
) {

    // 엔티티 → DTO 변환 (컨트롤러/서비스 계층에서 new SignupResponse(member) 로 호출)
    public static SignupResponse from(Member member) {
        return new SignupResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getCreatedAt()
        );
    }
}
