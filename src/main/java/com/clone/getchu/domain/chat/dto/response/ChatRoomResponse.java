package com.clone.getchu.domain.chat.dto.response;

public record ChatRoomResponse(
        Long chatRoomId,
        boolean created // true = 신규 생성, false = 기존 채팅방 반환
) {
}
