package com.clone.getchu.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== AUTH (A) =====
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A001", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A003", "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A004", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 토큰입니다."),
    LOGGED_OUT_TOKEN(HttpStatus.UNAUTHORIZED, "A006", "로그아웃된 토큰입니다."),

    // ===== MEMBER (M) =====
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M002", "현재 비밀번호가 올바르지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "M003", "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
    // ===== CATEGORY (C) =====
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않는 카테고리입니다."),
    // ===== PRODUCT (P) =====
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 상품입니다."),
    PRODUCT_FORBIDDEN(HttpStatus.FORBIDDEN, "P002", "본인의 상품만 수정/삭제할 수 있습니다."),
    PRICE_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "P003", "예약중/거래중 상태에서는 가격을 수정할 수 없습니다."),
    DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "P004", "예약중/거래중 상태에서는 상품을 삭제할 수 없습니다."),
    SELF_RESERVATION(HttpStatus.BAD_REQUEST, "P005", "본인의 상품은 예약할 수 없습니다."),

    // ===== TRADE (T) =====
    TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 거래입니다."),
    ALREADY_RESERVED(HttpStatus.CONFLICT, "T002", "이미 예약된 상품입니다. 잠시 후 다시 확인해보세요."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "T003", "현재 상태에서는 해당 전이가 불가능합니다."),
    TRADE_FORBIDDEN(HttpStatus.FORBIDDEN, "T004", "해당 거래에 대한 권한이 없습니다."),
    LOCK_TIMEOUT(HttpStatus.CONFLICT, "T005", "요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),

    // ===== CHAT (C) =====
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않는 채팅방입니다."),
    SELF_CHAT(HttpStatus.BAD_REQUEST, "C002", "본인의 판매글에는 채팅을 시작할 수 없습니다."),
    CHAT_FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "해당 채팅방에 대한 권한이 없습니다."),

    // ===== REVIEW (R) =====
    REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "R001", "거래 완료(SOLD) 상태에서만 리뷰를 작성할 수 있습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "R002", "이미 작성한 리뷰가 있습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "R003", "해당 거래의 구매자만 리뷰를 작성할 수 있습니다."),

    // ===== LIKE (L) =====
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "L001", "이미 찜한 상품입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "L002", "찜하지 않은 상품입니다."),

    // ===== GLOBAL/COMMON (G) =====
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "G001", "입력값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "G002", "요청한 리소스를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "G003", "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G004", "서버 오류가 발생했습니다."),
    INVALID_CURSOR_FORMAT(HttpStatus.BAD_REQUEST, "G004", "커서 형식이 올바르지 않습니다.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
