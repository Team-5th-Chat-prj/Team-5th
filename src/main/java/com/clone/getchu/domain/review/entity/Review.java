package com.clone.getchu.domain.review.entity;

import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.global.common.BaseEntity;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.InvalidRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false, unique = true)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Member reviewer; // 리뷰 하는 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private Member reviewee; // 리뷰 받는 회원

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false, length = 500)
    private String content;

    @Builder
    private Review(Trade trade, Member reviewer, Member reviewee, BigDecimal rating, String content) {
        this.trade = trade;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.rating = rating;
        this.content = content;
    }
    // 리뷰 생성 메서드
    public static Review create(Trade trade, Member reviewer, Member reviewee, BigDecimal rating, String content) {
        validateRating(rating);
        return Review.builder()
                .trade(trade)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(rating)
                .content(content)
                .build();
    }
    private static void validateRating(BigDecimal rating) {
        if (rating.compareTo(BigDecimal.valueOf(0.5)) < 0 ||
                rating.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            throw new InvalidRequestException(ErrorCode.INVALID_RATING);
        }
        // 0.5 단위 체크
        BigDecimal remainder = rating.remainder(BigDecimal.valueOf(0.5));
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidRequestException(ErrorCode.INVALID_RATING);
        }
    }
}
