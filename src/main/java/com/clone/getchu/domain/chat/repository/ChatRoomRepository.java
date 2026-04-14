package com.clone.getchu.domain.chat.repository;

import com.clone.getchu.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 중복 채팅방 조회 (멱등 생성용)
    Optional<ChatRoom> findByBuyerIdAndProductId(Long buyerId, Long productId);

    // 내 채팅방 목록 조회 (구매자 또는 판매자로 참여한 채팅방, 최신 메시지 순)
    List<ChatRoom> findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(Long buyerId, Long sellerId);
}
