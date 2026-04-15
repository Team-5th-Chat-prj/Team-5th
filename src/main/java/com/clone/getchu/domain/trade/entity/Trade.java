package com.clone.getchu.domain.trade.entity;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.global.common.BaseEntity;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.ForbiddenException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Trade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeStatus status;

    private LocalDateTime reservedAt;
    private LocalDateTime tradedAt;
    private LocalDateTime soldAt;

    //전이: SALE → RESERVED, RESERVED → TRADING, TRADING → SOLD
    public void proceed() {
        this.status = this.status.next();
        recordTimestamp(this.status);
    }

    //취소: RESERVED → SALE, TRADING → SALE
    public void cancel() {
        this.status = this.status.cancel();
        clearTimestamps(); //취소시 기록 시간 초기화
    }

    //거래 참여자 검증
    public void validateParticipant(Long memberId) {
        if (!isBuyer(memberId) && !isSeller(memberId)) {
            throw new ForbiddenException(ErrorCode.TRADE_FORBIDDEN);
        }
    }
    public boolean isBuyer(Long memberId){
        return this.buyer.getId().equals(memberId);
    }
    public boolean isSeller(Long memberId){
        return this.seller.getId().equals(memberId);
    }

    //상태 전이 시점에 맞는 타임스탬프를 기록
    private void recordTimestamp(TradeStatus nextStatus) {
        LocalDateTime now = LocalDateTime.now();
        switch (nextStatus) {
            case RESERVED -> this.reservedAt = now;
            case TRADING  -> this.tradedAt   = now;
            case SOLD     -> this.soldAt      = now;
        }
    }

    //취소시 모든 거래 진행 관련 타임스탬프를 초기화
    private void clearTimestamps(){
        this.reservedAt = null;
        this.tradedAt = null;
        this.soldAt = null;
    }
}
