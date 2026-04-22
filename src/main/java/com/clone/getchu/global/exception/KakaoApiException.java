package com.clone.getchu.global.exception;

public class KakaoApiException extends BusinessException {

    public KakaoApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public KakaoApiException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
