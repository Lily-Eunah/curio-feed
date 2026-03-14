package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.ArticleFeedResponse;
import com.curiofeed.backend.api.dto.CursorPageResponse;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.repository.ArticleFeedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleFeedServiceTest {

    @Mock
    private ArticleFeedRepository feedRepository;

    @InjectMocks
    private ArticleFeedService feedService;

    // ── Helper ──────────────────────────────────────────

    private ArticleFeedResponse createFeedResponse(String id, Instant publishedAt) {
        return ArticleFeedResponse.builder()
                .id(id)
                .title("Test Article")
                .thumbnailUrl("https://example.com/thumb.jpg")
                .categoryName("Technology")
                .sourceName("Test Source")
                .publishedAt(publishedAt)
                .estimatedReadingTime(0)
                .build();
    }

    private List<ArticleFeedResponse> createFeedResponses(int count) {
        List<ArticleFeedResponse> list = new ArrayList<>();
        Instant base = Instant.parse("2026-03-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            list.add(createFeedResponse(
                    UUID.randomUUID().toString(),
                    base.minusSeconds(i * 3600L) // 1시간 간격
            ));
        }
        return list;
    }

    // ── 1. First Page Tests ─────────────────────────────

    @Nested
    @DisplayName("첫 페이지 요청 (cursor 없음)")
    class FirstPageTests {

        @Test
        @DisplayName("cursor가 null이면 findFeedFirstPage를 호출한다")
        void shouldReturnFirstPage_whenCursorIsNull() {
            // given
            when(feedRepository.findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(createFeedResponses(3));

            // when
            CursorPageResponse<ArticleFeedResponse> response = feedService.getFeed(null, 10);

            // then
            verify(feedRepository).findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class));
            assertThat(response.getData()).hasSize(3);
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("cursor가 빈 문자열이면 findFeedFirstPage를 호출한다")
        void shouldReturnFirstPage_whenCursorIsBlank() {
            // given
            when(feedRepository.findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(createFeedResponses(2));

            // when
            CursorPageResponse<ArticleFeedResponse> response = feedService.getFeed("", 10);

            // then
            verify(feedRepository).findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class));
            assertThat(response.getData()).hasSize(2);
        }
    }

    // ── 2. Cursor-Based Page Tests ──────────────────────

    @Nested
    @DisplayName("커서 기반 페이지 요청")
    class CursorPageTests {

        @Test
        @DisplayName("유효한 cursor가 주어지면 findFeedByCursor에 올바른 Instant와 UUID를 전달한다")
        void shouldReturnNextPage_whenCursorProvided() {
            // given
            Instant cursorAt = Instant.parse("2026-03-01T12:00:00Z");
            UUID cursorId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            String cursorToken = cursorAt.toString() + "_" + cursorId.toString();

            when(feedRepository.findFeedByCursor(
                    eq(ArticleStatus.PUBLISHED),
                    eq(cursorAt),
                    eq(cursorId),
                    any(PageRequest.class)
            )).thenReturn(createFeedResponses(5));

            // when
            feedService.getFeed(cursorToken, 10);

            // then
            verify(feedRepository).findFeedByCursor(
                    eq(ArticleStatus.PUBLISHED),
                    eq(cursorAt),
                    eq(cursorId),
                    any(PageRequest.class)
            );
        }
    }

    // ── 3. Pagination Logic Tests ───────────────────────

    @Nested
    @DisplayName("hasNext 판별 로직")
    class PaginationLogicTests {

        @Test
        @DisplayName("결과가 size+1개이면 hasNext=true이고 마지막 항목은 제거된다")
        void shouldSetHasNextTrue_whenResultsExceedSize() {
            // given
            int size = 10;
            List<ArticleFeedResponse> elevenResults = createFeedResponses(size + 1);
            when(feedRepository.findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(elevenResults);

            // when
            CursorPageResponse<ArticleFeedResponse> response = feedService.getFeed(null, size);

            // then
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.getData()).hasSize(size);
        }

        @Test
        @DisplayName("결과가 size 이하이면 hasNext=false이고 nextCursor는 null이다")
        void shouldSetHasNextFalse_whenResultsEqualOrLessThanSize() {
            // given
            when(feedRepository.findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(createFeedResponses(7));

            // when
            CursorPageResponse<ArticleFeedResponse> response = feedService.getFeed(null, 10);

            // then
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("다음 페이지가 존재할 때 nextCursor는 '{publishedAt}_{id}' 형식이다")
        void shouldBuildCorrectNextCursor() {
            // given
            int size = 5;
            Instant expectedPublishedAt = Instant.parse("2026-03-01T00:00:00Z");
            String expectedId = UUID.randomUUID().toString();

            List<ArticleFeedResponse> results = createFeedResponses(size);
            // 마지막 항목(인덱스 size-1)의 값을 고정하여 커서 형식 검증
            ArticleFeedResponse lastItem = createFeedResponse(expectedId, expectedPublishedAt);
            results.set(size - 1, lastItem);
            // size+1번째 항목 추가 → hasNext=true 트리거
            results.add(createFeedResponse(UUID.randomUUID().toString(), expectedPublishedAt.minusSeconds(3600)));

            when(feedRepository.findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(results);

            // when
            CursorPageResponse<ArticleFeedResponse> response = feedService.getFeed(null, size);

            // then
            assertThat(response.getNextCursor())
                    .isEqualTo(expectedPublishedAt.toString() + "_" + expectedId);
        }
    }

    // ── 4. Edge Cases ───────────────────────────────────

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCaseTests {

        @Test
        @DisplayName("게시된 기사가 없으면 빈 데이터와 hasNext=false를 반환한다")
        void shouldReturnEmptyData_whenNoArticlesExist() {
            // given
            when(feedRepository.findFeedFirstPage(eq(ArticleStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            CursorPageResponse<ArticleFeedResponse> response = feedService.getFeed(null, 10);

            // then
            assertThat(response.getData()).isEmpty();
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("잘못된 cursor 형식이 주어지면 예외가 발생한다")
        void shouldThrowException_whenCursorFormatInvalid() {
            // when & then
            assertThatThrownBy(() -> feedService.getFeed("invalid-cursor", 10))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("cursor의 timestamp는 유효하지만 UUID가 DB에 없는 경우 정상 처리된다")
        void shouldHandleGracefully_whenCursorIdNotExists() {
            // given
            Instant cursorAt = Instant.parse("2026-03-01T12:00:00Z");
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            String cursorToken = cursorAt.toString() + "_" + nonExistentId.toString();

            when(feedRepository.findFeedByCursor(
                    eq(ArticleStatus.PUBLISHED),
                    eq(cursorAt),
                    eq(nonExistentId),
                    any(PageRequest.class)
            )).thenReturn(Collections.emptyList());

            // when
            CursorPageResponse<ArticleFeedResponse> response = feedService.getFeed(cursorToken, 10);

            // then — 500 에러 없이 빈 결과를 정상 반환
            assertThat(response.getData()).isEmpty();
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }
    }
}
