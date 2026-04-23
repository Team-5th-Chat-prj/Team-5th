package com.clone.getchu.domain.member.dto.response;

import com.clone.getchu.domain.member.entity.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        BigDecimal averageRating,
        int reviewCount,
        LocalDateTime createdAt,
        String locationName,
        Integer locationRadius,
        Double locationLat,
        Double locationLng
) {
    public static MemberResponse from(Member member) {
        var location = member.getLocation();
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getAverageRating(),
                member.getReviewCount(),
                member.getCreatedAt(),
                member.getLocationName(),
                member.getLocationRadius(),
                location != null ? location.getY() : null,
                location != null ? location.getX() : null
        );
    }
}
