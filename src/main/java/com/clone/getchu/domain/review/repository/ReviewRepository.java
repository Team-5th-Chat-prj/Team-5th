package com.clone.getchu.domain.review.repository;

import com.clone.getchu.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 거래 Id 멱등성 검증
    boolean existsByTradeId(Long tradeId);
    // 리뷰받은 사람 페이징 조회 / 최신순(비로그인 조회가능)
    Page<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId, Pageable pageable);
}
