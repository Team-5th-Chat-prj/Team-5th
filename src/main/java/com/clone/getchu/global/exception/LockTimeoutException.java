package com.clone.getchu.global.exception;

public class LockTimeoutException extends ConflictException {

    // 분산락 획득 실패 시 클라이언트에게 안내하는 재시도 권장 시간 (초)
    private static final int RETRY_AFTER_SECONDS = 1;

    public LockTimeoutException() {
        super(ErrorCode.LOCK_TIMEOUT, RETRY_AFTER_SECONDS);
    }
}
