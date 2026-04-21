package com.clone.getchu.domain.chat.dto.response;

public record ChatRoomSummaryResponse(
        Long chatRoomId,
        Long opponentId,
        String opponentNickname,
        Long productId,
        String opponentProfileImageUrl,
        String lastMessage,
        long unreadCount
) {
}
