package com.clone.getchu.domain.member.dto.response;

import com.clone.getchu.domain.member.entity.Member;

import java.math.BigDecimal;

public record MemberProfileResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        BigDecimal averageRating,
        int reviewCount
        // 리뷰 목록은 ReviewController에서 별도 API로
) {
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getAverageRating(),
                member.getReviewCount()
        );
    }
}