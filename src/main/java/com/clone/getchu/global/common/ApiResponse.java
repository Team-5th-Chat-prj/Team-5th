package com.clone.getchu.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";

    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static <T> ApiResponse<T> success() {
        // 사용: 삭제, 로그아웃처럼 응답 데이터가 없을 때
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        // 사용: 조회, 수정처럼 응답 데이터가 있을 때 (가장 많이 씀)
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        // 사용: "비밀번호가 변경되었습니다." 같은 커스텀 메시지가 필요할 때
        return new ApiResponse<>(SUCCESS_CODE, message, data);
    }
}
