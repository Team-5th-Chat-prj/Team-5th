package com.clone.getchu.domain.review.service;

import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.domain.review.dto.request.ReviewRequest;
import com.clone.getchu.domain.review.dto.response.ReviewResponse;
import com.clone.getchu.domain.review.entity.Review;
import com.clone.getchu.domain.review.repository.ReviewRepository;
import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeRole;
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
    private final MemberRepository memberRepository;

    /**
     * 리뷰 작성
     * 1. 거래 완료(SOLD) 상태 검증
     * 2. 구매자 본인(trade.buyer) 검증
     * 3. 동일 거래 중복 리뷰 검증
     * 4. 리뷰 저장 + 판매자 평점 통계 업데이트 (단일 트랜잭션)
     *    - 판매자 조회 시 비관적 락 적용: 동시 리뷰 작성으로 인한 Lost Update 방지
     */
    @Transactional
    public void createReview(CustomUserDetails userDetails, Long tradeId, ReviewRequest request) {
        // buyer, seller를 함께 로딩하여 이후 프록시 초기화로 인한 추가 쿼리 방지
        Trade trade = tradeRepository.findWithBuyerAndSellerById(tradeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRADE_NOT_FOUND));

        // 거래 완료 상태만 리뷰 작성 가능
        if (trade.getStatus() != TradeStatus.SOLD) {
            throw new InvalidRequestException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // 해당 거래의 구매자(trade.buyer)만 리뷰 작성 가능
        if (!trade.isBuyer(userDetails.getMemberId())) {
            throw new ForbiddenException(ErrorCode.REVIEW_FORBIDDEN);
        }

        // 거래당 리뷰는 1건만 허용
        if (reviewRepository.existsByTradeId(tradeId)) {
            throw new ConflictException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 비관적 락을 먼저 획득하여 리뷰 저장과 평점 업데이트를 동일 락 범위 안에서 처리
        // — flush 이전에 락 범위를 확정하여 Lost Update 방지
        Member seller = memberRepository.findByIdWithLock(trade.getSeller().getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        Review review = Review.create(
                trade,
                trade.getBuyer(),   // reviewer = 구매자
                seller,             // reviewee = 판매자 (락으로 조회한 엔티티 재사용)
                request.rating(),
                request.content()
        );
        reviewRepository.save(review);
        seller.updateReviewStats(request.rating());
        trade.proceed(TradeRole.BUYER); // SOLD → REVIEWED
    }

    /**
     * 리뷰 목록 조회 (커서 기반 페이지네이션, 비로그인 가능)
     * cursor: base64(reviewId) — null이면 첫 페이지
     */
    public CursorPageResponse<ReviewResponse> getReviews(Long memberId, String cursor, int size) {
        Long cursorId = decodeCursor(cursor);

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

    /**
     * 작성한 리뷰 목록 조회 (커서 기반 페이지네이션, 비로그인 가능)
     * cursor: base64(reviewId) — null이면 첫 페이지
     */
    public CursorPageResponse<ReviewResponse> getWrittenReviews(Long memberId, String cursor, int size) {
        Long cursorId = decodeCursor(cursor);

        List<ReviewResponse> results = reviewRepository.findReviewsByReviewerIdWithCursor(
                memberId, cursorId, PageRequest.of(0, size + 1)
        );

        boolean hasNext = results.size() > size;
        List<ReviewResponse> content = hasNext ? results.subList(0, size) : results;
        String nextCursor = hasNext
                ? CursorUtil.encodeCursor(content.get(content.size() - 1).id().toString())
                : null;

        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }

    /**
     * 커서 디코딩
     * 잘못된 Base64 또는 숫자가 아닌 값이 들어오면 400으로 변환하여 500 방지
     */
    private Long decodeCursor(String cursor) {
        if (cursor == null) return null;
        try {
            return Long.parseLong(CursorUtil.decodeCursor(cursor));
        } catch (Exception e) {
            throw new InvalidRequestException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }
}
