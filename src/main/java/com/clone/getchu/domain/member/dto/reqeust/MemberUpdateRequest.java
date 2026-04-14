package com.clone.getchu.domain.member.dto.reqeust;

import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,
        String profileImageUrl
) {
    // 빈 문자열 방어
    public MemberUpdateRequest {
        if (nickname != null && nickname.isBlank()) nickname = null;
        if (profileImageUrl != null && profileImageUrl.isBlank()) profileImageUrl = null;
    }
}
