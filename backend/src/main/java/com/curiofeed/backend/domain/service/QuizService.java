package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.QuizAttemptRequest;
import com.curiofeed.backend.api.dto.QuizAttemptResponse;

import java.util.UUID;

public interface QuizService {
    QuizAttemptResponse attemptQuiz(UUID articleId, UUID contentId, UUID quizId, QuizAttemptRequest request);
}
