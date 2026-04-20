package com.clone.getchu.domain.like.service;

import com.clone.getchu.domain.like.entity.Like;
import com.clone.getchu.domain.like.repository.LikeRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.domain.product.dto.ProductListResponse;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createLike(Long productId, Long memberId) {
        //삭제된 이력 포함해서 전체 데이터 조회
        Like like = likeRepository.findByProductIdAndMemberIdIncludingDeleted(productId, memberId)
                .orElse(null);

        if (like != null) {
            if (!like.isDeleted()) {
                throw new BusinessException(ErrorCode.LIKE_ALREADY_EXISTS);
            }
            //이전에 취소한 이력이 있다면 다시 활성화
            like.restore();
        } else {
            //처음 찜하는 경우
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

            likeRepository.save(new Like(product, member));
        }

        likeRepository.incrementLikeCount(productId);
    }

    @Transactional
    public void deleteLike(Long productId, Long memberId) {
        Like like = likeRepository.findByProductIdAndMemberIdIncludingDeleted(productId, memberId)
                .filter(l -> !l.isDeleted())
                .orElseThrow(() -> new NotFoundException(ErrorCode.LIKE_NOT_FOUND));

        like.softDelete();

        // 테이블의 likeCount 감소
        likeRepository.decrementLikeCount(productId);
    }

    @Transactional(readOnly = true)
    public Page<ProductListResponse> getMyLikesList(Long memberId, Pageable pageable) {
        Page<Like> likes = likeRepository.findAllByMemberId(memberId, pageable);

        return likes.map(like -> ProductListResponse.from(like.getProduct()));
    }
}
