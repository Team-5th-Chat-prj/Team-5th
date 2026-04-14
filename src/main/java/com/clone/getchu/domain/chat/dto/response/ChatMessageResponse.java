package com.clone.getchu.domain.chat.dto.response;

import com.clone.getchu.domain.chat.entity.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {

    private final Long messageId;
    private final Long chatRoomId;
    private final Long senderId;
    private final String senderNickname;
    private final String content;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    public ChatMessageResponse(ChatMessage message) {
        this.messageId = message.getId();
        this.chatRoomId = message.getChatRoomId();
        this.senderId = message.getSenderId();
        this.senderNickname = message.getSenderNickname();
        this.content = message.getContent();
        this.isRead = message.isRead();
        this.createdAt = message.getCreatedAt();
    }
}
