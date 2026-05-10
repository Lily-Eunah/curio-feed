package com.curiofeed.backend.api.dto.admin;

public record CategorySaveRequest(
        String name,
        String displayName,
        int sortOrder,
        boolean active
) {
}
