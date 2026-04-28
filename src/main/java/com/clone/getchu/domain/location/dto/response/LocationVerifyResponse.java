package com.clone.getchu.domain.location.dto.response;

import com.clone.getchu.domain.member.entity.Member;

/**
 * 동네 인증 완료 응답
 * locationName  : 인증된 행정동명 (예: "마포구 합정동")
 * locationRadius: 근처 상품 조회 반경 km (기본값 3)
 */
public record LocationVerifyResponse(
        String locationName,
        Integer locationRadius
) {
    public static LocationVerifyResponse from(Member member) {
        return new LocationVerifyResponse(
                member.getLocationName(),
                member.getLocationRadius()
        );
    }
}
