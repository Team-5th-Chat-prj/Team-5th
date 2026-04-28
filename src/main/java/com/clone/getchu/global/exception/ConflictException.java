package com.clone.getchu.global.exception;

import lombok.Getter;

@Getter
public class ConflictException extends BusinessException {

    private final Integer retryAfter;

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
        this.retryAfter = null;
    }

    public ConflictException(ErrorCode errorCode, int retryAfter) {
        super(errorCode);
        this.retryAfter = retryAfter;
    }
}
