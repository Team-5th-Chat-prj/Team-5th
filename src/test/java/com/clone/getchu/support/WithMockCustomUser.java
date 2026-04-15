package com.clone.getchu.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    long memberId() default 1L;

    String email() default "test@email.com";

    String nickname() default "테스트유저";

    String role() default "USER";
}