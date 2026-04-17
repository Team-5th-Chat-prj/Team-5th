package com.clone.getchu.global.lock;

import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private final RedisLockRepository redisLockRepository;

    private static final int MAX_RETRY = 10;
    private static final long INITIAL_BACKOFF_MS = 50;
    private static final long MAX_BACKOFF_MS = 1000;
    private static final Duration LOCK_EXPIRATION = Duration.ofSeconds(3);

    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        String value = UUID.randomUUID().toString();
        int retryCount = 0;
        long currentBackoff = INITIAL_BACKOFF_MS;

        while (retryCount < MAX_RETRY) {
            Boolean acquired = redisLockRepository.lock(key, value, LOCK_EXPIRATION);
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    return supplier.get();
                } finally {
                    redisLockRepository.unlock(key, value);
                }
            }

            try {
                Thread.sleep(currentBackoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }

            retryCount++;
            currentBackoff = Math.min(currentBackoff * 2, MAX_BACKOFF_MS);
        }

        // 락 획득 실패 시 이미 예약된 상품 예외로 반환 (혹은 timeout으로 간주 불가)
        throw new BusinessException(ErrorCode.ALREADY_RESERVED);
    }
}
