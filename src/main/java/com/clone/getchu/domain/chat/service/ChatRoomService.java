package com.clone.getchu.domain.chat.service;

import com.clone.getchu.domain.chat.dto.request.CreateChatRoomRequest;
import com.clone.getchu.domain.chat.dto.response.ChatRoomResponse;
import com.clone.getchu.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.clone.getchu.domain.chat.entity.ChatRoom;
import com.clone.getchu.domain.chat.repository.ChatMessageRepository;
import com.clone.getchu.domain.chat.repository.ChatRoomRepository;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.ForbiddenException;
import com.clone.getchu.global.exception.InvalidRequestException;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;

    /**
     * 채팅방 생성 (멱등)
     * - 동일 (buyerId, productId) 채팅방이 이미 존재하면 기존 채팅방 반환 (created=false)
     * - 없으면 새로 생성 (created=true)
     */
    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Long buyerId, CreateChatRoomRequest request) {
        // Product 조회하여 실제 sellerId 추출
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
        Long sellerId = product.getSeller().getId();

        // 본인 상품 채팅 시도 방지
        if (buyerId.equals(sellerId)) {
            throw new InvalidRequestException(ErrorCode.SELF_CHAT);
        }

        // 이미 존재하는 채팅방 확인 (멱등)
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByBuyerIdAndProductId(buyerId, request.productId());
        if (existingRoom.isPresent()) {
            ChatRoom room = existingRoom.get();
            // 구매자가 이전에 방을 나갔을 경우 재입장 처리 후 즉시 DB 반영
            room.reenterRoom(buyerId);
            room.reenterRoom(sellerId);
            chatRoomRepository.saveAndFlush(room);
            return new ChatRoomResponse(room.getId(), false);
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
            throw new ForbiddenException(ErrorCode.CHAT_FORBIDDEN);
        }

        return chatRoom;
    }

    /**
     * 채팅방 존재 확인 및 활성 참여자(나가지 않은 상태) 검증
     * - 메시지 이력 조회, 무한 스크롤, STOMP 구독 등 '읽기' 권한 검증에 사용
     */
    @Transactional(readOnly = true)
    public ChatRoom validateActiveChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = validateAndGetChatRoom(chatRoomId, memberId);

        if (chatRoom.isLeftBy(memberId)) {
            throw new ForbiddenException(ErrorCode.CHAT_ALREADY_LEFT);
        }

        return chatRoom;
    }

    /**
     * 채팅방 나가기 (Soft Delete)
     * - DB에서 삭제하지 않고, 나갔다는 플래그만 변경
     * - 채팅방 존재 여부만 확인하고, 참여자 권한 검증은 엔티티의 leaveRoom()에 위임
     * - saveAndFlush()로 영속성 컨텍스트를 즉시 DB에 반영하여 JPQL 쿼리와의 비동기화 방지
     */
    @Transactional
    public void leaveChatRoom(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        chatRoom.leaveRoom(memberId);
        chatRoomRepository.saveAndFlush(chatRoom); // Soft Delete 플래그 즉시 DB 반영
    }
}
