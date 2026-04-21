package com.clone.getchu.domain.chat.service;

import com.clone.getchu.domain.chat.dto.response.ChatMessageReadEvent;
import com.clone.getchu.domain.chat.entity.ChatRoom;
import com.clone.getchu.domain.chat.repository.ChatMessageRepository;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("채팅방 읽음 처리 및 실시간 이벤트 발송 성공")
    void markMessagesAsRead_Success() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 2L;

        // 권한 검증 시 에러가 나지 않도록 빈 정상 객체 반환
        ChatRoom mockRoom = ChatRoom.builder().productId(100L).buyerId(memberId).sellerId(3L).build();
        given(chatRoomService.validateActiveChatRoom(chatRoomId, memberId)).willReturn(mockRoom);

        // when
        chatMessageService.markMessagesAsRead(chatRoomId, memberId);

        // then
        // 1. DB의 isRead 업데이트 쿼리가 잘 호출되는지 검증
        verify(chatMessageRepository).markMessagesAsRead(chatRoomId, memberId);

        // 2. 실시간 STOMP 브로드캐스트가 정확한 타입 객체로 날아가는지 검증
        verify(messagingTemplate).convertAndSend(eq("/topic/room.1"), any(ChatMessageReadEvent.class));
    }

    @Test
    @DisplayName("채팅방 읽음 처리 실패 - 권한 없음 (타인의 채팅방)")
    void markMessagesAsRead_Fail_Forbidden() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 999L; // 권한 없는 유저

        given(chatRoomService.validateActiveChatRoom(chatRoomId, memberId))
                .willThrow(new ForbiddenException(ErrorCode.CHAT_FORBIDDEN));

        // when & then
        assertThrows(ForbiddenException.class, () -> chatMessageService.markMessagesAsRead(chatRoomId, memberId));

        // 예외 발생 시 뒤의 로직(DB 업데이트, 웹소켓 발송)이 호출되지 않았는지 철저히 검증
        verify(chatMessageRepository, never()).markMessagesAsRead(any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
