package com.clone.getchu.global.security;

import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.UnauthorizedException;
import com.clone.getchu.global.util.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * [WebSocket JWT 인증 인터셉터] STOMP 연결 시 토큰 검증
 *
 * HTTP 요청은 JwtAuthFilter가 처리하지만, WebSocket은 최초 연결(Upgrade) 이후
 * HTTP 필터 체인을 거치지 않습니다. 그래서 WebSocket 전용 인증 처리가 필요합니다.
 *
 * WebSocketConfig에서 configureClientInboundChannel()로 등록되어
 * 클라이언트 → 서버 방향의 모든 STOMP 메시지 전송 전에 실행됩니다.
 *
 * 클라이언트는 연결 시 아래와 같이 토큰을 전달해야 합니다:
 * STOMP CONNECT 헤더: { Authorization: "Bearer {AccessToken}" }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate stringRedisTemplate;

    // 메시지가 채널로 전송되기 직전에 호출됨
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 명령일 때만 검증 - SEND, SUBSCRIBE 등 이후 메시지는 세션에 user가 이미 설정됨
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            if (!jwtProvider.validateToken(token)) {
                throw new UnauthorizedException(ErrorCode.TOKEN_INVALID);
            }

            // 로그아웃된 토큰(블랙리스트) 차단 — HTTP 필터와 동일한 기준 적용
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.blacklistKey(token)))) {
                throw new UnauthorizedException(ErrorCode.LOGGED_OUT_TOKEN);
            }

            Authentication auth = jwtProvider.getAuthentication(token);
            // STOMP 세션에 인증 정보 등록 - 이후 convertAndSendToUser() 시 이 user 정보를 사용해 라우팅
            accessor.setUser(auth);
            log.debug("WebSocket CONNECT 인증 성공 - user: {}", auth.getName());
        }

        return message;
    }
}
