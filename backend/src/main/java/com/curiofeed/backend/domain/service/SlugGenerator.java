package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.repository.ArticleRepository;
import org.springframework.stereotype.Component;

@Component
public class SlugGenerator {

    private final ArticleRepository articleRepository;

    public SlugGenerator(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /**
     * 제목에서 slug를 생성하고 DB 중복 시 suffix(-2, -3...) 추가.
     */
    public String generate(String originalTitle) {
        String base = toSlugBase(originalTitle);
        if (base.isEmpty()) {
            base = "article";
        }

        String candidate = base;
        int suffix = 2;
        while (articleRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String toSlugBase(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // 영문/숫자/공백만 남김
                .trim()
                .replaceAll("\\s+", "-")         // 공백 → 하이픈
                .replaceAll("-{2,}", "-");        // 연속 하이픈 단일화
    }
}
