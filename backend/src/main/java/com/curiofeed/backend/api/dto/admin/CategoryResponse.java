package com.curiofeed.backend.api.dto.admin;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        int sortOrder,
        boolean active
) {}
