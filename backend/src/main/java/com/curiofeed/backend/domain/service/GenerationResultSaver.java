package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Transactional
    public void saveContent(UUID articleId, DifficultyLevel level, String content) {
        ArticleContent articleContent = contentRepository.findByArticleIdAndLevel(articleId, level)
                .orElseGet(() -> {
                    System.out.println("DEBUG: GenerationResultSaver: Article not found in contentRepository. Fetching from articleRepository with ID: " + articleId);
                    Article article = articleRepository.findById(articleId)
                            .orElseThrow(() -> {
                                System.err.println("DEBUG: GenerationResultSaver: FATAL: Article not found in articleRepository: " + articleId);
                                return new IllegalArgumentException("Article not found: " + articleId);
                            });
                    var ac = ArticleContent.create(article, level, content);
                    return contentRepository.save(ac);
                });
        
        // Clear existing related data when content is updated
        vocabularyRepository.deleteAllByArticleContentId(articleContent.getId());
        quizRepository.deleteAllByArticleContentId(articleContent.getId());
        vocabularyRepository.flush();
        quizRepository.flush();
        
        articleContent.updateContent(content);
        contentRepository.save(articleContent);
    }

    @Transactional
    public void saveVocab(UUID articleId, DifficultyLevel level, 
                          List<GenerationResult.VocabularyData> vocabs, 
                          com.curiofeed.backend.infrastructure.llm.validation.VocabLemmatizer lemmatizer) {
        if (vocabs == null || vocabs.isEmpty()) return;
        ArticleContent articleContent = contentRepository.findByArticleIdAndLevel(articleId, level)
                .orElseThrow(() -> new IllegalStateException("ArticleContent must exist before saving vocab"));
        
        vocabularyRepository.deleteAllByArticleContentId(articleContent.getId());
        vocabularyRepository.flush();
        
        for (var v : vocabs) {
            String displayWord = lemmatizer.normalizeDisplayWord(v.word());
            vocabularyRepository.save(Vocabulary.create(articleContent, displayWord, v.definition(), v.exampleSentence()));
        }
    }

    @Transactional
    public void saveQuiz(UUID articleId, DifficultyLevel level, List<GenerationResult.QuizData> quizzes) {
        if (quizzes == null || quizzes.isEmpty()) return;
        ArticleContent articleContent = contentRepository.findByArticleIdAndLevel(articleId, level)
                .orElseThrow(() -> new IllegalStateException("ArticleContent must exist before saving quizzes"));
        
        quizRepository.deleteAllByArticleContentId(articleContent.getId());
        quizRepository.flush();
        
        for (var q : quizzes) {
            quizRepository.save(Quiz.create(articleContent, q.type(), q.question(), q.options(),
                    q.correctAnswer(), q.explanation()));
        }
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

        // 2. ArticleContent 저장
        saveContent(articleId, level, result.content());
        ArticleContent articleContent = contentRepository.findByArticleIdAndLevel(articleId, level).get();

        // 3. vocab 없으면 CONTENT_ONLY
        if (!result.hasVocabularies()) {
            return SaveStatus.CONTENT_ONLY;
        }

        // 4. Vocabulary 저장 (Manual saving here because we don't have lemmatizer in this method usually, 
        // but for full save we can assume it's already lemmatized or use a simple save)
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

