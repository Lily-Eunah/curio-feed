package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class GenerationResultSaver {

    private final ArticleRepository articleRepository;
    private final ArticleContentRepository contentRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuizRepository quizRepository;

    public GenerationResultSaver(
            ArticleRepository articleRepository,
            ArticleContentRepository contentRepository,
            VocabularyRepository vocabularyRepository,
            QuizRepository quizRepository) {
        this.articleRepository = articleRepository;
        this.contentRepository = contentRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.quizRepository = quizRepository;
    }

    /**
     * 하나의 난이도(level)에 대한 content + vocabularies + quizzes를 단일 트랜잭션으로 저장.
     * 예외 발생 시 전체 롤백.
     */
    @Transactional
    public SaveStatus save(UUID articleId, DifficultyLevel level, GenerationResult result) {
        // 1. content 없으면 즉시 반환
        if (!result.hasContent()) {
            return SaveStatus.NO_CONTENT;
        }

        // 2. ArticleContent 저장 (safe update: 기존 존재하면 delete children → update content)
        ArticleContent articleContent;
        Optional<ArticleContent> existing = contentRepository.findByArticleIdAndLevel(articleId, level);
        if (existing.isPresent()) {
            articleContent = existing.get();
            // 기존 연관 데이터 삭제 (delete-then-insert 대신 children만 삭제)
            vocabularyRepository.deleteAllByArticleContentId(articleContent.getId());
            quizRepository.deleteAllByArticleContentId(articleContent.getId());
            vocabularyRepository.flush();
            quizRepository.flush();
            articleContent.updateContent(result.content());
        } else {
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));
            articleContent = ArticleContent.create(article, level, result.content());
            contentRepository.save(articleContent);
        }

        // 3. vocab 없으면 CONTENT_ONLY
        if (!result.hasVocabularies()) {
            return SaveStatus.CONTENT_ONLY;
        }

        // 4. Vocabulary 저장
        for (var vd : result.vocabularies()) {
            vocabularyRepository.save(
                    Vocabulary.create(articleContent, vd.word(), vd.definition(), vd.exampleSentence()));
        }

        // 5. quiz 없으면 CONTENT_WITH_VOCAB
        if (!result.hasQuizzes()) {
            return SaveStatus.CONTENT_WITH_VOCAB;
        }

        // 6. Quiz 저장
        for (var qd : result.quizzes()) {
            quizRepository.save(
                    Quiz.create(articleContent, qd.type(), qd.question(), qd.options(), qd.correctAnswer(), qd.explanation()));
        }

        return SaveStatus.FULL_SUCCESS;
    }
}
