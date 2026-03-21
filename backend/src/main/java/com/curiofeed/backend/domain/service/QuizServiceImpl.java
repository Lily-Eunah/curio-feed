package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.QuizAttemptRequest;
import com.curiofeed.backend.api.dto.QuizAttemptResponse;
import com.curiofeed.backend.domain.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;

    @Override
    public QuizAttemptResponse attemptQuiz(UUID quizId, QuizAttemptRequest request) {
        // TODO: intentionally failing for Red phase by returning null.
        return null;
    }
}
