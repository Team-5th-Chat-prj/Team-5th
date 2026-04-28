package com.clone.getchu.domain.chat.repository;

import com.clone.getchu.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 중복 채팅방 조회 (멱등 생성용)
    Optional<ChatRoom> findByBuyerIdAndProductId(Long buyerId, Long productId);

    // 내 채팅방 목록 조회 (구매자 또는 판매자로 참여한 채팅방, 최신 메시지 순)
    List<ChatRoom> findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(Long buyerId, Long sellerId);

    // 채팅방 목록 최적화 조회 (N+1 해결)
    @Query("SELECT new com.clone.getchu.domain.chat.dto.response.ChatRoomSummaryResponse(" +
           "cr.id, CASE WHEN cr.buyerId = :memberId THEN cr.sellerId ELSE cr.buyerId END, " +
           "COALESCE(m.nickname, '탈퇴한 회원'), cr.productId, m.profileImageUrl, " +
           "(SELECT cm.content FROM ChatMessage cm WHERE cm.id = (SELECT MAX(cm2.id) FROM ChatMessage cm2 WHERE cm2.chatRoomId = cr.id)), " +
           "(SELECT COUNT(cm2) FROM ChatMessage cm2 WHERE cm2.chatRoomId = cr.id AND cm2.senderId <> :memberId AND cm2.isRead = false)" +
           ") " +
           "FROM ChatRoom cr " +
           "LEFT JOIN Member m ON (CASE WHEN cr.buyerId = :memberId THEN cr.sellerId ELSE cr.buyerId END) = m.id " +
           "WHERE (cr.buyerId = :memberId AND cr.deletedByBuyer = false) OR (cr.sellerId = :memberId AND cr.deletedBySeller = false) " +
           "ORDER BY cr.lastMessageAt DESC")
    List<com.clone.getchu.domain.chat.dto.response.ChatRoomSummaryResponse> findMyChatRoomSummaries(@Param("memberId") Long memberId);
}
