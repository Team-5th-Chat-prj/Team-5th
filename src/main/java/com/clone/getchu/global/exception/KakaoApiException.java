package com.clone.getchu.global.exception;

public class KakaoApiException extends BusinessException {

    public KakaoApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public KakaoApiException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    public KakaoApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause.getMessage(), cause);
    }
}
