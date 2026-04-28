package com.clone.getchu.domain.member.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,
        // 빈 문자열("")을 허용: 프론트가 ""를 보내면 프로필 이미지 삭제 의도로 해석
        // null은 "변경 의사 없음"으로 해석 (패턴: null = 미전송, "" = 명시적 삭제, URL/Base64 = 업데이트)
        @Pattern(regexp = "^(https?://.*|data:image/.*)?$", message = "올바른 이미지 형식이어야 합니다.")
        String profileImageUrl
) {
    public MemberUpdateRequest {
        // nickname만 빈 문자열 → null 정규화 (닉네임은 삭제 개념이 없으므로 무시)
        // profileImageUrl은 변환하지 않음: ""(빈 문자열) = 이미지 삭제 신호로 Member.update()에서 처리
        if (nickname != null && nickname.isBlank()) nickname = null;
    }
}
