package com.clone.getchu.global.common;

import com.clone.getchu.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;

@Getter
// null인 필드는 JSON 응답에서 제외 - errors, retryAfter는 상황에 따라 없을 수 있어서 적용
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;       // 도메인 접두사 포함 에러 코드 (예: "A002", "P001")
    private final String message;    // 클라이언트에게 보여줄 에러 메시지
    private final String timestamp;  // 에러 발생 시각 (ISO-8601 형식)
    private List<FieldErrorDetail> errors;  // @Valid 유효성 검증 실패 시 필드별 오류 목록 (없으면 null → JSON 미포함)
    private Integer retryAfter;             // 재시도 가능 시간(초), 분산락 타임아웃 등 409 응답 시에만 포함 (없으면 null → JSON 미포함)

    // 생성자를 private으로 막고 정적 팩토리 메서드만 허용해서 일관된 방식으로만 생성되게 강제
    private ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    // 일반 비즈니스 예외 - ErrorCode에 정의된 기본 메시지 사용
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    // 상세 메시지가 있는 경우 - 기본 메시지 대신 detailMessage로 덮어씀
    public static ErrorResponse of(ErrorCode errorCode, String detailMessage) {
        return new ErrorResponse(errorCode.getCode(), detailMessage);
    }

    // 409 Conflict + 재시도 시간 포함 - 분산락 획득 실패(LockTimeoutException) 등에서 사용
    public static ErrorResponse of(ErrorCode errorCode, Integer retryAfter) {
        ErrorResponse response = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        response.retryAfter = retryAfter;
        return response;
    }

    // @Valid 유효성 검증 실패 전용 - 어떤 필드가 왜 잘못됐는지 목록으로 담아서 반환
    public static ErrorResponse ofValidation(List<FieldError> fieldErrors) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.INVALID_REQUEST.getCode(),
            ErrorCode.INVALID_REQUEST.getMessage()
        );
        response.errors = fieldErrors.stream()
            .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return response;
    }

    // 유효성 검증 실패 시 개별 필드 오류를 담는 내부 클래스
    // 예: { "field": "price", "message": "0 이상이어야 합니다" }
    @Getter
    @AllArgsConstructor
    public static class FieldErrorDetail {
        private final String field;    // 오류가 발생한 필드명
        private final String message;  // 해당 필드의 오류 메시지
    }
}
