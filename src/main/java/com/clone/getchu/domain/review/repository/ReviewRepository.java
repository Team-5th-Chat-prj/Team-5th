package com.clone.getchu.domain.review.repository;

import com.clone.getchu.domain.review.dto.response.ReviewResponse;
import com.clone.getchu.domain.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 거래 Id 멱등성 검증
    boolean existsByTradeId(Long tradeId);

    // 리뷰받은 회원의 리뷰 목록 커서 페이징 조회 (최신순)
    // - JOIN으로 reviewer, trade→product를 한 번에 로딩하여 N+1 방지
    // - cursorId가 null이면 첫 페이지, 있으면 해당 id 미만의 데이터 조회
    @Query("SELECT new com.clone.getchu.domain.review.dto.response.ReviewResponse(" +
            "r.id, r.rating, r.content, r.createdAt, " +
            "rv.nickname, rv.profileImageUrl, p.title) " +
            "FROM Review r " +
            "JOIN r.reviewer rv " +
            "JOIN r.trade t " +
            "JOIN t.product p " +
            "WHERE r.reviewee.id = :revieweeId " +
            "AND (:cursorId IS NULL OR r.id < :cursorId) " +
            "ORDER BY r.id DESC")
    List<ReviewResponse> findReviewsByRevieweeIdWithCursor(
            @Param("revieweeId") Long revieweeId,
            @Param("cursorId") Long cursorId,
            Pageable pageable);
}
