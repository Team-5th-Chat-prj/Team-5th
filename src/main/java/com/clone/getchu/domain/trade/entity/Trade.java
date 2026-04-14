package com.clone.getchu.domain.trade.entity;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Trade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeStatus status;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "traded_at")
    private LocalDateTime tradedAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    //전이: SALE → RESERVED, RESERVED → TRADING, TRADING → SOLD
    public void proceed() {
        this.status = this.status.next();
        recordTimestamp(this.status);
    }

    //취소 가능: RESERVED → SALE, TRADING → SALE
    public void cancel() {
        this.status = this.status.cancel();
    }

    //상태 전이 시점에 맞는 타임스탬프를 기록합니다.
    private void recordTimestamp(TradeStatus nextStatus) {
        LocalDateTime now = LocalDateTime.now();
        switch (nextStatus) {
            case RESERVED -> this.reservedAt = now;
            case TRADING  -> this.tradedAt   = now;
            case SOLD     -> this.soldAt      = now;
            default -> { /* SALE(취소)은 타임스탬프 불필요 */ }
        }
    }
}
