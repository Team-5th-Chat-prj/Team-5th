package com.clone.getchu.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {

    private final Long chatRoomId;
    private final boolean created; // true = 신규 생성, false = 기존 채팅방 반환
}
