package com.curiofeed.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CursorPageResponse<T> {

    private List<T> data;
    private String nextCursor; // Null when there is no next page
    private boolean hasNext;
}
