package com.clone.getchu.domain.chat.controller;

import com.clone.getchu.domain.chat.dto.request.CreateChatRoomRequest;
import com.clone.getchu.domain.chat.dto.response.ChatMessageResponse;
import com.clone.getchu.domain.chat.dto.response.ChatRoomResponse;
import com.clone.getchu.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.clone.getchu.domain.chat.service.ChatMessageService;
import com.clone.getchu.domain.chat.service.ChatRoomService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    /**
     * POST /chat-rooms
     * 채팅방 생성 (멱등)
     * - 신규: 201 Created
     * - 기존: 200 OK
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetChatRoom(
            @Valid @RequestBody CreateChatRoomRequest request
    ) {
        Long buyerId = SecurityUtil.getCurrentMemberId();
        ChatRoomResponse response = chatRoomService.createOrGetChatRoom(buyerId, request.getSellerId(), request);

        if (response.isCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("채팅방이 생성되었습니다.", response));
        }
        return ResponseEntity.ok(ApiResponse.success("기존 채팅방을 반환합니다.", response));
    }

    /**
     * GET /chat-rooms
     * 내 채팅방 목록 조회 (구매자 또는 판매자로 참여한 채팅방)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> getMyChatRooms() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<ChatRoomSummaryResponse> response = chatRoomService.getMyChatRooms(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /chat-rooms/{chatRoomId}/messages?cursor={cursor}&size={size}
     * 채팅 이력 조회 (Cursor 기반 페이지네이션)
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<CursorPageResponse<ChatMessageResponse>>> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        CursorPageResponse<ChatMessageResponse> response =
                chatMessageService.getMessages(chatRoomId, memberId, cursor, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
