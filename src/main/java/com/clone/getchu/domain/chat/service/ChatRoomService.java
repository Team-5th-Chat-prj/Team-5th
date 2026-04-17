package com.clone.getchu.domain.chat.service;

import com.clone.getchu.domain.chat.dto.request.CreateChatRoomRequest;
import com.clone.getchu.domain.chat.dto.response.ChatRoomResponse;
import com.clone.getchu.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.clone.getchu.domain.chat.entity.ChatRoom;
import com.clone.getchu.domain.chat.repository.ChatMessageRepository;
import com.clone.getchu.domain.chat.repository.ChatRoomRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.InvalidRequestException;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    /**
     * 채팅방 생성 (멱등)
     * - 동일 (buyerId, productId) 채팅방이 이미 존재하면 기존 채팅방 반환 (created=false)
     * - 없으면 새로 생성 (created=true)
     *
     * NOTE: Product 도메인이 없으므로 현재 sellerId를 요청자가 직접 전달하지 않음.
     *       Product 도메인 완성 시 productRepository로 sellerId 조회 후 자동 주입.
     *       임시로 productId를 sellerId로 사용하는 구조 대신, sellerId를 request에 포함.
     */
    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Long buyerId, Long sellerId, CreateChatRoomRequest request) {
        // 본인 상품 채팅 시도 방지
        if (buyerId.equals(sellerId)) {
            throw new InvalidRequestException(ErrorCode.SELF_CHAT);
        }

        // 이미 존재하는 채팅방 확인 (멱등)
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByBuyerIdAndProductId(buyerId, request.productId());
        if (existingRoom.isPresent()) {
            return new ChatRoomResponse(existingRoom.get().getId(), false);
        }

        // 새 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .productId(request.productId())
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();
        chatRoomRepository.save(chatRoom);
        return new ChatRoomResponse(chatRoom.getId(), true);
    }

    /**
     * 내 채팅방 목록 조회
     * - 구매자 또는 판매자로 참여한 채팅방 반환 (최신 메시지 순)
     */
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getMyChatRooms(Long memberId) {
        return chatRoomRepository.findMyChatRoomSummaries(memberId);
    }

    /**
     * 채팅방 존재 확인 및 참여자 검증 (다른 서비스에서 공통 사용)
     */
    @Transactional(readOnly = true)
    public ChatRoom validateAndGetChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoom.getBuyerId().equals(memberId) && !chatRoom.getSellerId().equals(memberId)) {
            throw new com.clone.getchu.global.exception.ForbiddenException(ErrorCode.CHAT_FORBIDDEN);
        }

        return chatRoom;
    }

    /**
     * 채팅방 나가기 (Soft Delete)
     * - DB에서 삭제하지 않고, 나갔다는 플래그만 변경
     */
    @Transactional
    public void leaveChatRoom(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = validateAndGetChatRoom(chatRoomId, memberId);
        chatRoom.leaveRoom(memberId);
    }
}
