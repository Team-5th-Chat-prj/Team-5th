package com.clone.getchu.global.lock;

import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private final RedisLockRepository redisLockRepository;

    @Value("${lock.retry.max:120}")
    private int maxRetry;

    @Value("${lock.retry.initial-backoff-ms:50}")
    private long initialBackoffMs;

    @Value("${lock.retry.max-backoff-ms:1000}")
    private long maxBackoffMs;

    @Value("${lock.expiration-seconds:3}")
    private long lockExpirationSeconds;

    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        String value = UUID.randomUUID().toString();
        int retryCount = 0;
        long currentBackoff = initialBackoffMs;

        while (retryCount < maxRetry) {
            Boolean acquired = redisLockRepository.lock(key, value, Duration.ofSeconds(lockExpirationSeconds));
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
            currentBackoff = Math.min(currentBackoff * 2, maxBackoffMs);
        }

        // 락 획득 실패 시 이미 예약된 상품 예외로 반환 (혹은 timeout으로 간주 불가)
        throw new BusinessException(ErrorCode.LOCK_TIMEOUT);
    }
}
