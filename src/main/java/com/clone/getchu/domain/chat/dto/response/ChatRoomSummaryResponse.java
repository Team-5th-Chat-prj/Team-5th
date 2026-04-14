package com.clone.getchu.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomSummaryResponse {

    private final Long chatRoomId;
    private final Long opponentId;
    private final String opponentNickname;
    private final Long productId;
    private final String lastMessage;
    private final long unreadCount;
}
