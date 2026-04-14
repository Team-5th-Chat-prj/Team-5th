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
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByBuyerIdAndProductId(buyerId, request.getProductId());
        if (existingRoom.isPresent()) {
            return new ChatRoomResponse(existingRoom.get().getId(), false);
        }

        // 새 채팅방 생성
        ChatRoom chatRoom = ChatRoom.create(request.getProductId(), buyerId, sellerId);
        chatRoomRepository.save(chatRoom);
        return new ChatRoomResponse(chatRoom.getId(), true);
    }

    /**
     * 내 채팅방 목록 조회
     * - 구매자 또는 판매자로 참여한 채팅방 반환 (최신 메시지 순)
     */
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getMyChatRooms(Long memberId) {
        List<ChatRoom> chatRooms = chatRoomRepository
                .findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(memberId, memberId);

        return chatRooms.stream()
                .map(room -> {
                    boolean isBuyer = room.getBuyerId().equals(memberId);
                    Long opponentId = isBuyer ? room.getSellerId() : room.getBuyerId();

                    // 상대방 닉네임 조회
                    String opponentNickname = memberRepository.findById(opponentId)
                            .map(Member::getNickname)
                            .orElse("탈퇴한 회원");

                    // 마지막 메시지 조회
                    String lastMessage = chatMessageRepository
                            .findByChatRoomIdOrderByIdDesc(room.getId(),
                                    org.springframework.data.domain.PageRequest.of(0, 1))
                            .stream()
                            .findFirst()
                            .map(msg -> msg.getContent())
                            .orElse("");

                    // 읽지 않은 메시지 수
                    long unreadCount = chatMessageRepository
                            .countByChatRoomIdAndSenderIdNotAndIsReadFalse(room.getId(), memberId);

                    return new ChatRoomSummaryResponse(
                            room.getId(),
                            opponentId,
                            opponentNickname,
                            room.getProductId(),
                            lastMessage,
                            unreadCount
                    );
                })
                .collect(Collectors.toList());
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
}
