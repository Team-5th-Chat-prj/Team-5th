package com.clone.getchu.domain.auth.dto;

import com.clone.getchu.domain.member.entity.Member;

import java.time.LocalDateTime;

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
