package com.clone.getchu.domain.chat.entity;

import com.clone.getchu.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "chat_message",
        indexes = @Index(name = "idx_chat_message_room_id_desc", columnList = "chat_room_id, id DESC")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_nickname", nullable = false)
    private String senderNickname;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // 정적 팩토리 메서드
    public static ChatMessage create(Long chatRoomId, Long senderId, String senderNickname, String content) {
        ChatMessage message = new ChatMessage();
        message.chatRoomId = chatRoomId;
        message.senderId = senderId;
        message.senderNickname = senderNickname;
        message.content = content;
        message.isRead = false;
        return message;
    }
}
