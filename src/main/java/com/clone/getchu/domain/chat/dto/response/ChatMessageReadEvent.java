package com.clone.getchu.domain.chat.dto.response;

public record ChatMessageReadEvent(
        String type,
        Long chatRoomId,
        Long readerId
) {
    public static ChatMessageReadEvent of(Long chatRoomId, Long readerId) {
        return new ChatMessageReadEvent("READ", chatRoomId, readerId);
    }
}
