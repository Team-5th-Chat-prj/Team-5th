package com.clone.getchu.domain.member.dto.response;

import com.clone.getchu.domain.member.entity.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberProfileResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        BigDecimal averageRating,
        int reviewCount,
        LocalDateTime createdAt
) {
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getAverageRating(),
                member.getReviewCount(),
                member.getCreatedAt()
        );
    }
}