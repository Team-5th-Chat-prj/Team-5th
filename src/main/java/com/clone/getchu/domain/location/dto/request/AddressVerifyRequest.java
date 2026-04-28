package com.clone.getchu.domain.location.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 주소 텍스트 기반 동네 인증 요청
 * address: 검색할 주소 문자열 (예: "서울 마포구 합정동")
 */
public record AddressVerifyRequest(

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 200, message = "주소는 200자 이하로 입력해주세요.")
        String address
) {}
