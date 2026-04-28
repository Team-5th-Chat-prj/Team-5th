package com.clone.getchu.global.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorUtil {

    private CursorUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String encodeCursor(String rawCursor) {
        if (rawCursor == null) return null;
        return Base64.getEncoder().encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeCursor(String encodedCursor) {
        if (encodedCursor == null) return null;
        return new String(Base64.getDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
    }
}
