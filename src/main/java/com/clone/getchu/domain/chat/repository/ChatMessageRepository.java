package com.clone.getchu.domain.chat.repository;

import com.clone.getchu.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Cursor 기반 페이지네이션: cursor(messageId) 이전 메시지 조회
    List<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long chatRoomId, Long cursor, Pageable pageable);

    // 첫 페이지 조회 (cursor 없을 때)
    List<ChatMessage> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

    // 읽지 않은 메시지 수 (상대방이 보낸 메시지 중 isRead=false)
    long countByChatRoomIdAndSenderIdNotAndIsReadFalse(Long chatRoomId, Long myId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.chatRoomId = :chatRoomId AND m.senderId != :memberId AND m.isRead = false")
    void markMessagesAsRead(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId);
}
