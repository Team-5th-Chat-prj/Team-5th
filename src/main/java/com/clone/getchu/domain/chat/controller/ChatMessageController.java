package com.clone.getchu.domain.chat.controller;

import com.clone.getchu.domain.chat.dto.request.ChatMessageRequest;
import com.clone.getchu.domain.chat.service.ChatMessageService;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * STOMP 메시지 수신 엔드포인트
     * 클라이언트: SEND destination=/app/chat.sendMessage
     * 브로드캐스트: /topic/room.{chatRoomId}
     *
     * StompChannelInterceptor에서 accessor.setUser(authentication) 처리됨.
     * principal은 UsernamePasswordAuthenticationToken이며
     * getPrincipal()로 CustomUserDetails를 꺼낼 수 있음.
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid @Payload ChatMessageRequest request, Principal principal) {
        CustomUserDetails userDetails = extractUserDetails(principal);
        chatMessageService.sendMessage(request, userDetails.getMemberId(), userDetails.getNickname());
    }

    /**
     * STOMP Principal에서 CustomUserDetails 추출
     * StompChannelInterceptor가 Authentication을 setUser()로 등록하므로
     * Principal은 항상 Authentication의 구현체임.
     */
    private CustomUserDetails extractUserDetails(Principal principal) {
        if (principal instanceof Authentication auth
                && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw new IllegalStateException("STOMP 인증 정보가 올바르지 않습니다.");
    }
}
