package com.curiofeed.backend.api.controller;

import com.curiofeed.backend.api.dto.QuizAttemptRequest;
import com.curiofeed.backend.api.dto.QuizAttemptResponse;
import com.curiofeed.backend.domain.service.QuizService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizAttemptController.class)
@Import(com.curiofeed.backend.api.controller.advice.GlobalExceptionHandler.class)
class QuizAttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuizService quizService;

    @Test
    @DisplayName("퀴즈 정답 제출 시 정확한 응답 규격(isCorrect, correctAnswer, explanation)을 반환한다 - 단일 정답")
    void shouldReturnCorrectContract_whenSubmittedSingleAnswer() throws Exception {
        // given
        UUID quizId = UUID.randomUUID();
        QuizAttemptRequest request = new QuizAttemptRequest("A");

        QuizAttemptResponse mockResponse = QuizAttemptResponse.builder()
                .isCorrect(false)
                .correctAnswer("B")
                .explanation("선택하신 'A'는 오답이며, 정답은 'B'입니다.")
                .build();

        given(quizService.attemptQuiz(eq(quizId), any(QuizAttemptRequest.class)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/quizzes/{id}/attempt", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCorrect").value(false))
                .andExpect(jsonPath("$.correctAnswer").value("B"))
                .andExpect(jsonPath("$.explanation").value("선택하신 'A'는 오답이며, 정답은 'B'입니다."));
    }

    @Test
    @DisplayName("퀴즈 정답 제출 시 정확한 응답 규격(isCorrect, correctAnswer, explanation)을 반환한다 - 배열 정답(재배열 등)")
    void shouldReturnCorrectContract_whenSubmittedArrayAnswer() throws Exception {
        // given
        UUID quizId = UUID.randomUUID();
        // Array submission
        QuizAttemptRequest request = new QuizAttemptRequest(List.of("London", "in", "live", "I"));

        QuizAttemptResponse mockResponse = QuizAttemptResponse.builder()
                .isCorrect(false)
                .correctAnswer(List.of("I", "live", "in", "London"))
                .explanation("주어 + 동사 + 전치사구 순서로 구성됩니다.")
                .build();

        given(quizService.attemptQuiz(eq(quizId), any(QuizAttemptRequest.class)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/quizzes/{id}/attempt", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCorrect").value(false))
                .andExpect(jsonPath("$.correctAnswer[0]").value("I"))
                .andExpect(jsonPath("$.correctAnswer[3]").value("London"))
                .andExpect(jsonPath("$.explanation").value("주어 + 동사 + 전치사구 순서로 구성됩니다."));
    }
}
