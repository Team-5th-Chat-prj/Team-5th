package com.clone.getchu.global.lock;

import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private final RedisLockRepository redisLockRepository;

    @Value("${lock.retry.max:10}")
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
                    // 현재 진행중인 트랜잭션이 있는지 확인 (있다면 락 해제하지 않음)
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                // 커밋이든 롤백이든 트랜잭션이 완전히 종료된 후에 락을 가장 마지막에 해제
                                redisLockRepository.unlock(key, value);
                            }
                        });
                    } else {
                        // 현재 시작된 트랜잭션이 없다면 즉각 해제 (Facade 패턴일 경우 이쪽을 탐)
                        redisLockRepository.unlock(key, value);
                    }
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
        throw new BusinessException(ErrorCode.ALREADY_RESERVED);
    }
}
