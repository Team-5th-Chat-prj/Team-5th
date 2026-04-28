package com.clone.getchu.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateChatRoomRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId
) {
}
