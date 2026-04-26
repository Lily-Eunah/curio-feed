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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "article_contents",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_article_level",
        columnNames = {"article_id", "level"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleContent extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String audioUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @OneToMany(mappedBy = "articleContent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vocabulary> vocabularies = new ArrayList<>();

    @OneToMany(mappedBy = "articleContent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quiz> quizzes = new ArrayList<>();

    public static ArticleContent create(Article article, DifficultyLevel level, String content) {
        ArticleContent ac = new ArticleContent();
        ac.article = article;
        ac.level = level;
        ac.content = content;
        return ac;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
