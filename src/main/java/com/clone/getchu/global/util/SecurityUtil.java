package com.clone.getchu.global.util;

import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.UnauthorizedException;
import com.clone.getchu.global.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() 
            || authentication.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED); 
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getMemberId();
    }
}
