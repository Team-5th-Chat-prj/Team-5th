package com.clone.getchu.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 평점 유효성 검증 커스텀 어노테이션
 * - 허용 범위: 0.5 ~ 5.0
 * - 허용 단위: 0.5 단위 (0.5, 1.0, 1.5, ... 5.0)
 *
 * null 처리는 별도 @NotNull로 위임하므로 null은 통과시킵니다.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RatingValidator.class)
public @interface ValidRating {
    String message() default "평점은 0.5 ~ 5.0 사이의 0.5 단위 값만 가능합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
