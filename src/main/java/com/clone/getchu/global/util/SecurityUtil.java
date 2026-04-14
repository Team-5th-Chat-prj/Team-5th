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
        return getAuthenticatedUserDetails().getMemberId();
    }

    public static String getCurrentMemberNickname() {
        return getAuthenticatedUserDetails().getNickname();
    }

    private static CustomUserDetails getAuthenticatedUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }
}
