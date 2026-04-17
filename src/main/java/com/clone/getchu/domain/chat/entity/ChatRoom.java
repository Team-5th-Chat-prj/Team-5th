package com.clone.getchu.domain.chat.entity;

import com.clone.getchu.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_buyer_product",
                columnNames = {"buyer_id", "product_id"}
        ),
        indexes = @Index(name = "idx_chat_room_seller_id", columnList = "seller_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Product 도메인이 완성될 때까지 FK만 보유 (연관관계 제외)
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "deleted_by_buyer", nullable = false)
    private boolean deletedByBuyer = false;

    @Column(name = "deleted_by_seller", nullable = false)
    private boolean deletedBySeller = false;

    @Builder
    private ChatRoom(Long productId, Long buyerId, Long sellerId) {
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
    }

    public void updateLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public void leaveRoom(Long memberId) {
        if (this.buyerId.equals(memberId)) {
            this.deletedByBuyer = true;
        } else if (this.sellerId.equals(memberId)) {
            this.deletedBySeller = true;
        }
    }

    public void reenterRoom() {
        this.deletedByBuyer = false;
        this.deletedBySeller = false;
    }
}
