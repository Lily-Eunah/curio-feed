package com.curiofeed.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "vocabularies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vocabulary extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String exampleSentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_content_id", nullable = false)
    private ArticleContent articleContent;

    public static Vocabulary create(ArticleContent content, String word, String definition, String exampleSentence) {
        Vocabulary v = new Vocabulary();
        v.articleContent = content;
        v.word = word;
        v.definition = definition;
        v.exampleSentence = exampleSentence;
        return v;
    }
}
