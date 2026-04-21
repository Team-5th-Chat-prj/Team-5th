package com.clone.getchu.domain.chat.service;

import com.clone.getchu.domain.chat.repository.ChatMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
    @DisplayName("채팅방 읽음 처리 및 실시간 이벤트 발송 테스트")
    void markMessagesAsRead_Success() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 2L;

        // 권한 체크 Mocking (에러 발생하지 않도록 빈 동작 혹은 반환)
        given(chatRoomService.validateActiveChatRoom(chatRoomId, memberId)).willReturn(null);

        // when
        chatMessageService.markMessagesAsRead(chatRoomId, memberId);

        // then
        // 1. DB의 isRead 업데이트 쿼리가 잘 호출되는지 검증
        verify(chatMessageRepository).markMessagesAsRead(chatRoomId, memberId);

        // 2. 실시간 STOMP 브로드캐스트가 정확한 토픽으로 잘 날아가는지 검증 (카카오톡 숫자 1 없애기)
        verify(messagingTemplate).convertAndSend(eq("/topic/room.1"), any(Map.class));
    }
}
