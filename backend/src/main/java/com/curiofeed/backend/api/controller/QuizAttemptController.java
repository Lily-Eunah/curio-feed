package com.curiofeed.backend.api.controller;

import com.curiofeed.backend.api.dto.QuizAttemptRequest;
import com.curiofeed.backend.api.dto.QuizAttemptResponse;
import com.curiofeed.backend.domain.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizService quizService;

    @PostMapping("/{id}/attempt")
    public ResponseEntity<QuizAttemptResponse> attemptQuiz(
            @PathVariable UUID id,
            @RequestBody QuizAttemptRequest request
    ) {
        QuizAttemptResponse response = quizService.attemptQuiz(id, request);
        return ResponseEntity.ok(response);
    }
}
