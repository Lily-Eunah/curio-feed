package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.QuizAttemptRequest;
import com.curiofeed.backend.api.dto.QuizAttemptResponse;
import com.curiofeed.backend.domain.entity.Quiz;
import com.curiofeed.backend.domain.repository.QuizRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;

    @Override
    public QuizAttemptResponse attemptQuiz(UUID quizId, QuizAttemptRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found with id: " + quizId));

        return quiz.evaluate(request.getAnswer());
    }
}
