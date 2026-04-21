package com.clone.getchu.domain.like.service;

import com.clone.getchu.global.lock.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class LikeFacade {

    private final LockService lockService;
    private final LikeService likeService;

    //찜하기
    public void createLike(Long productId, Long memberId) {
        String lockKey = "lock:product:"+productId;

        lockService.executeWithLock(lockKey, () -> {
            likeService.createLike(productId, memberId);
            return null;
        });
    }

    //찜삭제
    public void deleteLike(Long productId, Long memberId){
        String lockKey = "lock:product:"+productId+":member:"+memberId;

        lockService.executeWithLock(lockKey, () -> {
            likeService.deleteLike(productId, memberId);
            return null;
        });
    }
}
