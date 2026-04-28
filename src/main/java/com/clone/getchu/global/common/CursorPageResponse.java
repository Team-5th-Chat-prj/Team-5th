package com.clone.getchu.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CursorPageResponse<T> {
    private final List<T> content;
    private final String nextCursor;
    private final boolean hasNext;
}
