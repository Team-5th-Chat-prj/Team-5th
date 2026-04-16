package com.clone.getchu.domain.chat.dto.response;

import com.clone.getchu.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String senderNickname,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getSenderNickname(),
                message.getContent(),
                message.isRead(),
                message.getCreatedAt()
        );
    }
}
