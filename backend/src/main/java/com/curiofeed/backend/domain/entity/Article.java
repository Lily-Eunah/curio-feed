package com.curiofeed.backend.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String originalTitle;

    @Column(nullable = false)
    private String sourceName;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(nullable = false)
    private Instant originalPublishedAt;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleStatus status;

    @Column(columnDefinition = "TEXT")
    private String originalContent;

    @Version
    private long version;

    @Column(nullable = false)
    private long viewCount = 0;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArticleContent> contents = new ArrayList<>();

    public static Article create(
            String originalTitle,
            String sourceName,
            String sourceUrl,
            String originalContent,
            Instant originalPublishedAt,
            Category category,
            String slug
    ) {
        Article article = new Article();
        article.originalTitle = originalTitle;
        article.sourceName = sourceName;
        article.sourceUrl = sourceUrl;
        article.originalContent = originalContent;
        article.originalPublishedAt = originalPublishedAt;
        article.title = originalTitle;
        article.slug = slug;
        article.category = category;
        article.publishedAt = originalPublishedAt;
        article.status = ArticleStatus.DRAFT;
        return article;
    }

    public void updateStatus(ArticleStatus newStatus) {
        this.status = newStatus;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article)) return false;
        Article that = (Article) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
