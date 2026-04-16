package com.clone.getchu.domain.review.service;

import com.clone.getchu.domain.review.dto.request.ReviewRequest;
import com.clone.getchu.domain.review.dto.response.ReviewResponse;
import com.clone.getchu.domain.review.entity.Review;
import com.clone.getchu.domain.review.repository.ReviewRepository;
import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.trade.repository.TradeRepository;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.exception.*;
import com.clone.getchu.global.security.CustomUserDetails;
import com.clone.getchu.global.util.CursorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TradeRepository tradeRepository;

    /**
     * 리뷰 작성
     * 1. 거래 완료(SOLD) 상태 검증
     * 2. 구매자 본인(trade.member) 검증
     * 3. 동일 거래 중복 리뷰 검증
     * 4. 리뷰 저장 + 판매자 평점 통계 업데이트 (단일 트랜잭션)
     */
    @Transactional
    public void createReview(CustomUserDetails userDetails, Long tradeId, ReviewRequest request) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRADE_NOT_FOUND));

        // 거래 완료 상태만 리뷰 작성 가능
        if (trade.getStatus() != TradeStatus.SOLD) {
            throw new InvalidRequestException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // 해당 거래의 구매자(trade.buyer)만 리뷰 작성 가능
        if (!trade.getBuyer().getId().equals(userDetails.getMemberId())) {
            throw new ForbiddenException(ErrorCode.REVIEW_FORBIDDEN);
        }

        // 거래당 리뷰는 1건만 허용
        if (reviewRepository.existsByTradeId(tradeId)) {
            throw new ConflictException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.create(
                trade,
                trade.getBuyer(),
                trade.getSeller(),
                request.rating(),
                request.content()
        );
        reviewRepository.save(review);

        // 판매자 평점 통계 업데이트 (같은 트랜잭션 내 dirty checking으로 반영)
        trade.getSeller().updateReviewStats(request.rating());
    }

    /**
     * 리뷰 목록 조회 (커서 기반 페이지네이션, 비로그인 가능)
     * cursor: base64(reviewId) — null이면 첫 페이지
     */
    public CursorPageResponse<ReviewResponse> getReviews(Long memberId, String cursor, int size) {
        Long cursorId = (cursor != null) ? Long.parseLong(CursorUtil.decodeCursor(cursor)) : null;

        // size + 1개를 조회하여 다음 페이지 존재 여부 판단
        List<ReviewResponse> results = reviewRepository.findReviewsByRevieweeIdWithCursor(
                memberId, cursorId, PageRequest.of(0, size + 1)
        );

        boolean hasNext = results.size() > size;
        List<ReviewResponse> content = hasNext ? results.subList(0, size) : results;
        String nextCursor = hasNext
                ? CursorUtil.encodeCursor(content.get(content.size() - 1).id().toString())
                : null;

        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
