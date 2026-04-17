package com.clone.getchu.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class RatingValidator implements ConstraintValidator<ValidRating, BigDecimal> {

    private static final BigDecimal MIN = BigDecimal.valueOf(0.5);
    private static final BigDecimal MAX = BigDecimal.valueOf(5.0);
    private static final BigDecimal STEP = BigDecimal.valueOf(0.5);

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // null은 @NotNull에 위임 — 여기서는 통과
        if (value == null) return true;

        // 범위 검증 (0.5 ~ 5.0)
        if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) return false;

        // 0.5 단위 검증
        return value.remainder(STEP).compareTo(BigDecimal.ZERO) == 0;
    }
}
