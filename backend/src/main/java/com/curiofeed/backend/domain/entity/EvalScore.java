package com.curiofeed.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "eval_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvalScore extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_content_id", nullable = false)
    private ArticleContent articleContent;

    @Column(nullable = false, length = 100)
    private String evaluatorModel;

    @Column(nullable = false)
    private double factualAccuracy;

    @Column(nullable = false)
    private double levelAppropriateness;

    @Column(nullable = false)
    private double engagement;

    @Column(nullable = false)
    private double safety;

    @Column(nullable = false)
    private double overall;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    public EvalScore(ArticleContent articleContent, String evaluatorModel, double factualAccuracy,
                     double levelAppropriateness, double engagement, double safety, double overall, String rawResponse) {
        this.articleContent = articleContent;
        this.evaluatorModel = evaluatorModel;
        this.factualAccuracy = factualAccuracy;
        this.levelAppropriateness = levelAppropriateness;
        this.engagement = engagement;
        this.safety = safety;
        this.overall = overall;
        this.rawResponse = rawResponse;
    }
}
