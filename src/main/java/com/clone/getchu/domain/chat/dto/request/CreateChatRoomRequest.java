package com.clone.getchu.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateChatRoomRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        // TODO: Product 도메인 완성 후 productRepository로 sellerId를 자동 조회하고 해당 필드 제거
        @NotNull(message = "판매자 ID는 필수입니다.")
        Long sellerId
) {
}
