package com.curiofeed.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "article_generation_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleGenerationJob extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(nullable = false)
    private UUID articleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;
    
    // Test constructor
    public ArticleGenerationJob(UUID articleId, JobStatus status) {
        this.articleId = articleId;
        this.status = status;
    }
}
