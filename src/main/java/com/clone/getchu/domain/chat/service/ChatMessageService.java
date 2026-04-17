package com.clone.getchu.domain.chat.service;

import com.clone.getchu.domain.chat.dto.request.ChatMessageRequest;
import com.clone.getchu.domain.chat.dto.response.ChatMessageResponse;
import com.clone.getchu.domain.chat.entity.ChatMessage;
import com.clone.getchu.domain.chat.entity.ChatRoom;
import com.clone.getchu.domain.chat.repository.ChatMessageRepository;
import com.clone.getchu.global.common.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int DEFAULT_PAGE_SIZE = 30;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 전송 (STOMP WebSocket)
     * 1. 채팅방 존재 + 참여자 검증
     * 2. DB 저장
     * 3. lastMessageAt 업데이트
     * 4. /topic/room.{chatRoomId} 브로드캐스트
     */
    @Transactional
    public void sendMessage(ChatMessageRequest request, Long senderId, String senderNickname) {
        ChatRoom chatRoom = chatRoomService.validateAndGetChatRoom(request.chatRoomId(), senderId);

        // 메시지 저장
        ChatMessage message = ChatMessage.builder()
                .chatRoomId(request.chatRoomId())
                .senderId(senderId)
                .senderNickname(senderNickname)
                .content(request.content())
                .build();
        chatMessageRepository.save(message);

        // lastMessageAt 갱신 & 메시지를 보낸 사람의 퇴장 상태만 복구
        chatRoom.updateLastMessageAt(LocalDateTime.now());
        chatRoom.reenterRoom(senderId);

        // STOMP 브로드캐스트 → /topic/room.{chatRoomId}
        ChatMessageResponse response = ChatMessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/room." + request.chatRoomId(), response);
    }

    /**
     * 채팅 이력 조회 (Cursor 기반 페이지네이션)
     * - cursor 없음: 최신 메시지부터 size개 조회
     * - cursor 있음: cursor(messageId) 이전 메시지부터 size개 조회
     * - size+1 조회 후 hasNext 판단, nextCursor = 마지막 messageId
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ChatMessageResponse> getMessages(Long chatRoomId, Long memberId, Long cursor, int size) {
        // 채팅방 존재 + 참여자 검증 + 나가지 않은 상태인지 검증
        chatRoomService.validateActiveChatRoom(chatRoomId, memberId);

        int fetchSize = size + 1; // hasNext 판단용 +1
        PageRequest pageable = PageRequest.of(0, fetchSize);

        List<ChatMessage> messages;
        if (cursor == null) {
            messages = chatMessageRepository.findByChatRoomIdOrderByIdDesc(chatRoomId, pageable);
        } else {
            messages = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(chatRoomId, cursor, pageable);
        }

        boolean hasNext = messages.size() == fetchSize;
        if (hasNext) {
            messages = messages.subList(0, size); // 실제 반환할 size개만
        }

        String nextCursor = (hasNext && !messages.isEmpty())
                ? String.valueOf(messages.get(messages.size() - 1).getId())
                : null;

        List<ChatMessageResponse> content = messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());

        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
