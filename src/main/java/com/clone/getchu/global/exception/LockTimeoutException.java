package com.clone.getchu.global.exception;

public class LockTimeoutException extends ConflictException {
    public LockTimeoutException() {
        super(ErrorCode.LOCK_TIMEOUT, 1);
    }
}
