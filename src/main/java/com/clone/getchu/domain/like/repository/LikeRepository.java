package com.clone.getchu.domain.like.repository;

import com.clone.getchu.domain.like.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    // 찜 여부 확인
    @Query(value = "SELECT * FROM likes l WHERE l.product_id = :productId AND l.member_id = :memberId",
            nativeQuery = true)
    Optional<Like> findByProductIdAndMemberIdIncludingDeleted(
            @Param("productId") Long productId,
            @Param("memberId") Long memberId);

    // 내 찜 목록 페이징 조회
    @Query(value = "SELECT l FROM Like l JOIN FETCH l.product WHERE l.member.id = :memberId AND l.isDeleted = false",
            countQuery = "SELECT COUNT(l) FROM Like l WHERE l.member.id = :memberId AND l.isDeleted = false")
    Page<Like> findAllByMemberId(Long memberId, Pageable pageable);

    //상품의 찜 수 증가
//    @Modifying(clearAutomatically = true)
//    @Query("UPDATE Product p SET p.likeCount = p.likeCount + 1 WHERE p.id = :productId")
//    void incrementLikeCount(@Param("productId") Long productId);

    //상품의 찜 수 감소
//    @Modifying(clearAutomatically = true)
//    @Query("UPDATE Product p SET p.likeCount = p.likeCount - 1 WHERE p.id = :productId AND p.likeCount > 0")
//    void decrementLikeCount(@Param("productId") Long productId);
}
