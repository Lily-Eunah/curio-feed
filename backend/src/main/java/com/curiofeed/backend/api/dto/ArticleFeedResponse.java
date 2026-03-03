package com.curiofeed.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleFeedResponse {
    
    private String id;
    private String title;
    private String thumbnailUrl;
    private String categoryName;
    private String sourceName;
    private Instant publishedAt;
    private int estimatedReadingTime; // in minutes
}
