# CurioFeed API Contract

Branch: `feature/frontend-backend-integration`

This document is the authoritative contract between the frontend and backend for the MVP integration.
It is verified against existing backend controllers, DTOs, service implementations, and tests.

---

## Contents

1. [Base URL](#base-url)
2. [Error Format](#error-format)
3. [Feed API](#feed-api)
4. [Article Detail API](#article-detail-api)
5. [Quiz Attempt API](#quiz-attempt-api)
6. [Next Article API](#next-article-api)
7. [Vocabulary DTO](#vocabulary-dto)
8. [Quiz DTO](#quiz-dto)
9. [Client-Side State (localStorage)](#client-side-state-localstorage)
10. [Required Backend DTO Additions](#required-backend-dto-additions)
11. [Unsupported Items](#unsupported-items)

---

## Base URL

```
VITE_API_BASE_URL=http://localhost:8080
```

All API paths are relative to this base URL. The frontend must read this from the environment variable and must never hardcode `localhost`.

---

## Error Format

Verified against `GlobalExceptionHandler` and `ArticleControllerTest`, `QuizAttemptControllerTest`.

```json
{
  "error": "Not Found",
  "message": "Article not found with id: <uuid>"
}
```

| HTTP Status | Trigger |
|---|---|
| 400 Bad Request | `IllegalArgumentException` (e.g. missing `choiceId` for MCQ) |
| 404 Not Found | `EntityNotFoundException` (article, content, or quiz not found) |

The frontend must handle both 400 and 404 gracefully (show error state, not crash).

---

## Feed API

**Endpoint:** `GET /api/articles`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `level` | enum | No | `EASY` | `EASY`, `MEDIUM`, or `HARD` — controls which level's content is used for `excerpt` and `estimatedReadingTime`; articles without content at this level are excluded from results |
| `category` | string | No | — | Category display name (e.g. `Tech`). Omit or pass `ALL` for no category filter |
| `cursor` | string | No | — | Opaque cursor token from previous response |
| `size` | int | No | `10` | Page size |

**Cursor token format** (internal, treat as opaque on frontend):
`{publishedAt ISO-8601 string}_{uuid}`

**Response:**

```json
{
  "data": [
    {
      "id": "018f1234-abcd-7xxx-xxxx-xxxxxxxxxxxx",
      "title": "The Age of Invisible Computers",
      "categoryName": "Tech",
      "sourceName": "The Atlantic",
      "thumbnailUrl": "https://...",
      "publishedAt": "2026-04-25T10:00:00Z",
      "estimatedReadingTime": 5,
      "excerpt": "Technology is disappearing into the background of our lives — and that might change everything."
    }
  ],
  "nextCursor": "2026-04-25T10:00:00Z_018f1234-abcd-7xxx-xxxx-xxxxxxxxxxxx",
  "hasNext": true
}
```

**Field notes:**

- `id`: UUID string (UUID v7, time-ordered)
- `publishedAt`: ISO-8601 string (`Instant` serialized by Jackson). Frontend converts to display label (Today / Yesterday / date string).
- `estimatedReadingTime`: integer, minutes. Derived server-side from content word count (`ceil(wordCount / 200)`). Not stored in DB.
- `excerpt`: short preview string (~160 characters). Derived server-side from the first paragraph of the `ArticleContent` that matches the requested `level`. Not stored in DB. Truncated with `…` if over limit. If the requested level's content is unavailable, the article is excluded from the feed (preferred behavior); `originalContent` is a last-resort fallback only and must not be primary behavior.
- `thumbnailUrl`: may be empty string `""`. Frontend must handle gracefully.
- `sourceName`: publication name string.

**Frontend display rules** (UI_POLICY §2.2):

Feed cards show: category dot, category label, title, excerpt, reading time.

Feed cards do NOT show: difficulty badge, published date, "5 words", "Read" label.

**Backend implementation notes:**

- `ArticleController` currently has no `level` or `category` parameters on `GET /api/articles` — both must be added. See [Required Backend DTO Additions](#required-backend-dto-additions).
- `ArticleFeedRepository` currently hardcodes `estimatedReadingTime = 0` and has no content join — the queries must be revised to join `article_contents` by level and optionally filter by category display name.
- `excerpt` and `estimatedReadingTime` are derived in the service layer from the joined content record, not from `originalContent`.
- No DB schema changes required.

**Verified against:** `ArticleControllerTest.shouldReturn200_andFeedResponse_whenGetFeed`

---

## Article Detail API

**Endpoint:** `GET /api/articles/{id}`

**Path Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | Article ID from feed response |

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `level` | enum | No | `EASY` | `EASY`, `MEDIUM`, or `HARD` |

**Response:**

```json
{
  "id": "018f1234-abcd-7xxx-xxxx-xxxxxxxxxxxx",
  "title": "The Age of Invisible Computers",
  "originalTitle": "The Original Title",
  "sourceName": "The Atlantic",
  "sourceUrl": "https://...",
  "thumbnailUrl": "https://...",
  "publishedAt": "2026-04-25T10:00:00Z",
  "categoryName": "Tech",
  "availableLevels": ["EASY", "MEDIUM", "HARD"],
  "content": {
    "id": "019a5678-efgh-7xxx-xxxx-xxxxxxxxxxxx",
    "level": "EASY",
    "content": "There was a time when computers were room-sized machines...",
    "audioUrl": null,
    "vocabularies": [...],
    "quizzes": [...]
  }
}
```

**Field notes:**

- `availableLevels`: list of difficulty levels for which content has been generated. The level switcher UI must only show levels present in this list.
- `content.id`: UUID of the `ArticleContent` row. **Required for quiz attempt URL.** See [Required Backend DTO Additions](#required-backend-dto-additions).
- `content.content`: plain text article body. No `{{word}}` template markers. The frontend must perform safe word-boundary text scanning to locate vocabulary words for inline highlighting. See [Vocabulary Handling](#vocabulary-dto).
- `content.audioUrl`: nullable. Not used in MVP (no full-article TTS per UI_POLICY §5.3).
- `publishedAt`: displayed in article meta line (e.g. "Apr 25, 2026").

**Backend implementation notes:**

- `ArticleDetailServiceImpl` builds this response from the `Article` and `ArticleContent` entities. Currently `content.id` is not included in the DTO — must be added (see below).
- Default level is `EASY` when omitted (verified: `ArticleControllerTest.shouldUseDefaultLevelEasy_whenLevelParamIsOmitted`).
- Returns 404 when article or content not found (verified: `ArticleControllerTest.shouldReturn404_whenServiceThrowsNotFoundException`).

**Verified against:** `ArticleControllerTest`, `ArticleDetailServiceImpl`

---

## Quiz Attempt API

**Endpoint:** `POST /api/articles/{articleId}/contents/{contentId}/quizzes/{quizId}/attempts`

**Path Parameters:**

| Parameter | Source |
|---|---|
| `articleId` | `ArticleDetailResponse.id` |
| `contentId` | `ArticleDetailResponse.content.id` (requires DTO addition) |
| `quizId` | `ArticleDetailResponse.content.quizzes[n].id` (requires DTO addition) |

**Request body:**

For MCQ:
```json
{ "choiceId": "A" }
```

For Short Answer (optional; MVP uses self-evaluation, not backend check):
```json
{ "answerText": "The fix sounds simple but requires deep behavior change." }
```

**Response:**

```json
{
  "isCorrect": true,
  "correctAnswer": "A",
  "explanation": "The article argues that computing is becoming ambient..."
}
```

For SCRAMBLE (not used in MVP), `correctAnswer` is an array: `["I", "live", "in", "London"]`.

**Frontend usage for MCQ (UI_POLICY §7.1):**

The frontend calls this endpoint on each attempt (up to 2 per MCQ question). It uses `isCorrect` and `explanation` from the response to drive the retry/done state. The frontend does NOT expose the `correctAnswer` value directly; it only uses it to highlight the correct choice after the second wrong attempt.

**Short Answer handling (UI_POLICY §7.3):**

Short answer is self-evaluated by the user. The frontend shows the model answer (from `QuizDto` — see below) alongside the user's typed answer and asks "Was your answer close? Yes / No". The quiz attempt API is NOT called for short answer in MVP.

**Verified against:** `QuizAttemptControllerTest` (all 4 test cases)

---

## Next Article API

### Preferred: Backend API (not yet implemented)

```
GET /api/articles/{id}/next?level=EASY
```

Expected response: a single `ArticleDetailResponse` or a minimal stub:
```json
{
  "id": "...",
  "title": "...",
  "categoryName": "...",
  "estimatedReadingTime": 4,
  "excerpt": "..."
}
```

Logic: same category, same level, older `publishedAt`.

**Status:** Out of scope for MVP. Do NOT implement this endpoint during the `feature/frontend-backend-integration` branch unless the feedCache fallback proves insufficient after real integration testing. The MVP fallback below is the primary implementation path.

### MVP Fallback: Client-Side Computation

If the backend endpoint does not exist, the frontend computes the next article from its in-memory feed cache:

```
nextArticle = feedCache
  .filter(a => a.categoryName === currentArticle.categoryName)
  .filter(a => a.publishedAt < currentArticle.publishedAt)
  .sort by publishedAt DESC
  [0]
```

The `nextArticle` is loaded fully via `GET /api/articles/{id}?level={currentLevel}` when the user taps it.

**UI rule (UI_POLICY §10):** Next article is always shown quietly near the bottom of Article Detail when a next article exists. It is not a gamified completion reward and is not conditional on quiz completion. Selection logic: same selected category (unless `All`), same current level, older `publishedAt`. The frontend resolves the next article from the feed cache if the backend endpoint is not yet available.

---

## Vocabulary DTO

Returned inside `ArticleDetailResponse.content.vocabularies`.

```json
{
  "word": "ambient",
  "definition": "Present in the surrounding environment in a quiet, unobtrusive way.",
  "exampleSentence": "The ambient music in the café made everyone feel relaxed."
}
```

**Field mapping (frontend type `VocabEntry`):**

| Backend field | Frontend field |
|---|---|
| `word` | `word` |
| `definition` | `definition` |
| `exampleSentence` | `example` |

**Vocabulary inline highlighting (UI_POLICY §5.1, safe text scanning):**

The article body (`content.content`) is plain text. The frontend must highlight vocab words using safe DOM-based text scanning:

1. Build a set of vocabulary words from the response.
2. For each paragraph, scan for whole-word occurrences using word-boundary matching (e.g. `\bword\b`, case-insensitive).
3. Wrap matched words in a tappable inline element with a dotted underline.
4. Do NOT use `innerHTML` or `dangerouslySetInnerHTML` for this operation.
5. If a word is not found in the paragraph, skip it silently — no error.
6. A vocabulary word that appears multiple times should be highlighted on all occurrences.

**TTS (UI_POLICY §5.3):** Vocabulary bottom sheet exposes a speaker button. TTS uses `window.speechSynthesis.speak()` scoped to the single vocab word. Must only fire on explicit user tap. Must never autoplay.

---

## Quiz DTO

Returned inside `ArticleDetailResponse.content.quizzes` as an ordered list.

```json
[
  {
    "id": "01ab1234-xxxx-7xxx-xxxx-xxxxxxxxxxxx",
    "type": "MULTIPLE_CHOICE",
    "question": "What is the main idea of this article?",
    "options": {
      "choices": [
        { "key": "A", "text": "Computers are getting smaller every year", "explanation": null },
        { "key": "B", "text": "Technology is becoming invisible but raises privacy questions", "explanation": null },
        { "key": "C", "text": "Scientists no longer use large computers", "explanation": null },
        { "key": "D", "text": "Smartphones are replacing all other devices", "explanation": null }
      ],
      "explanations": null
    }
  },
  {
    "id": "01ab5678-xxxx-7xxx-xxxx-xxxxxxxxxxxx",
    "type": "MULTIPLE_CHOICE",
    "question": "Which best matches 'ambient' as used in this article?",
    "options": { "choices": [...], "explanations": null }
  },
  {
    "id": "01ab9abc-xxxx-7xxx-xxxx-xxxxxxxxxxxx",
    "type": "SHORT_ANSWER",
    "question": "Why does the author call invisibility both a 'feature' and a 'risk'?",
    "options": {
      "choices": null,
      "explanations": {
        "modelAnswer": "It is a feature because it makes technology easier to use. It is a risk because hidden technology can collect data without users noticing."
      }
    }
  }
]
```

**Frontend mapping:**

| Backend | Frontend quiz key |
|---|---|
| First `MULTIPLE_CHOICE` in list | `q1` |
| Second `MULTIPLE_CHOICE` in list | `q2` |
| First `SHORT_ANSWER` in list | `q3` |

If fewer than expected quiz types are present, the frontend renders only what is available.

**Correct answer policy:** The `QuizDto` does NOT expose the correct choice key or index. The frontend determines correctness for MCQ exclusively by calling the quiz attempt API and reading `isCorrect` from the response. The frontend never reconstructs the correct answer before submission.

**Short answer model answer:** The frontend reads `options.explanations.modelAnswer` to display the reference answer after the user submits their own text response. The quiz attempt API is not called for short answer.

**`QuizDto.id`:** Required to build the quiz attempt URL. See [Required Backend DTO Additions](#required-backend-dto-additions).

**Verified against:** `QuizAttemptControllerTest`, `Quiz.evaluate()` in entity

---

## Client-Side State (localStorage)

All user interaction state is stored in `localStorage` under the key `curio_state`. No backend persistence for these fields in MVP.

```ts
interface AppState {
  onboarded: boolean;
  userLevel: 'EASY' | 'MEDIUM' | 'HARD';
  selectedCategory: string;         // 'All' | category display name
  savedIds: string[];               // article UUIDs
  readIds: string[];                // article UUIDs — quiz ≥1 answered
  visitedIds: string[];             // article UUIDs — entered article view
  continueReading: ContinueReadingState | null;
  quizProgress: Record<string, ArticleQuizProgress>;
  // Added for integration:
  feedCache: FeedArticleSummary[];  // cached feed items for next-article + saved screen
}

interface ContinueReadingState {
  articleId: string;
  scrollPosition: number;   // px, for scroll restore
  progress: number;         // 0–100, article body % seen (NOT displayed)
  updatedAt: number;        // Date.now() ms
}

interface FeedArticleSummary {
  id: string;
  title: string;
  categoryName: string;
  estimatedReadingTime: number;
  excerpt: string;
  publishedAt: string;      // ISO-8601 string
}

interface ArticleQuizProgress {
  q1?: MCQResult;
  q2?: MCQResult;
  q3?: ShortAnswerResult;
}

interface MCQResult {
  status: 'done';
  correct: boolean;
  choiceKey: string;        // the key submitted ('A'/'B'/etc.) — replaces index-based approach
  attempts: number;         // 1 or 2
}

interface ShortAnswerResult {
  status: 'done';
  selfEval: 'yes' | 'no';
  userText: string;
}
```

**State rules (UI_POLICY §11):**

| State | Trigger | Effect |
|---|---|---|
| `visitedIds` | Entering article view | Feed card opacity 0.5 |
| `readIds` | Answering ≥1 quiz | Removes from `continueReading` slot; no visual label |
| `continueReading` | ≥25% article body scroll | One slot only; newer qualifying article replaces old |
| `savedIds` | Tap save icon (feed or article) | Persisted in localStorage; no backend call |
| `quizProgress` | Each quiz answer | Persisted per `articleId`; partial (wrong-only) not saved |
| `feedCache` | Feed API response | Updated on each feed load; used for next-article and saved screen |

---

## Required Backend DTO Additions

These are the only backend code changes required to support frontend integration. No DB schema changes. No new endpoints are required at minimum.

### 1. `ArticleDetailResponse.ArticleContentDto` — add `id`

**File:** `backend/src/main/java/com/curiofeed/backend/api/dto/ArticleDetailResponse.java`

Add `UUID id` to `ArticleContentDto`. The value comes from `ArticleContent.getId()`, already available in `ArticleDetailServiceImpl.mapContent()`.

**Why required:** The quiz attempt URL is `POST /api/articles/{articleId}/contents/{contentId}/quizzes/{quizId}/attempts`. Without `contentId` in the response the frontend cannot construct this URL.

### 2. `ArticleDetailResponse.QuizDto` — add `id`

**File:** `backend/src/main/java/com/curiofeed/backend/api/dto/ArticleDetailResponse.java`

Add `UUID id` to `QuizDto`. The value comes from `Quiz.getId()`, already available during `mapContent()` quiz stream.

**Why required:** Same as above — `quizId` is required for the attempt URL.

### 3. `ArticleFeedResponse` — add `excerpt` and fix `estimatedReadingTime`

**File:** `backend/src/main/java/com/curiofeed/backend/api/dto/ArticleFeedResponse.java`

Add `String excerpt` field.

`excerpt` derivation rules (in priority order):
1. **Primary:** derive from `ArticleContent.content` at the requested `level`. Take the first sentence or first ~160 characters, strip trailing whitespace, append `…` if truncated.
2. **Exclusion (preferred fallback):** if no content record exists at the requested level, exclude the article from the feed entirely.
3. **Last resort only:** if `originalContent` must be used as a fallback, it must be documented explicitly at the call site and treated as a data pipeline gap, not normal behavior.

`estimatedReadingTime` derivation:
- Primary: `ceil(wordCount / 200)` where `wordCount` counts whitespace-delimited tokens in `ArticleContent.content` at the requested level.
- If level content is unavailable (and the article is excluded), this value is not applicable.
- Do NOT hardcode `5` — it is misleading for articles with differing length.

**Why required:** Feed cards must display excerpt (UI_POLICY §2.2) and reading time. Hardcoded `0` produces "0 min read" which is incorrect product behavior.

**DB schema:** No changes. Neither `excerpt` nor `estimatedReadingTime` is persisted.

---

### 4. `ArticleController` + `ArticleFeedService` + `ArticleFeedRepository` — add `level` and `category` support

**Files:**
- `backend/src/main/java/com/curiofeed/backend/api/controller/ArticleController.java`
- `backend/src/main/java/com/curiofeed/backend/domain/service/ArticleFeedService.java`
- `backend/src/main/java/com/curiofeed/backend/domain/repository/ArticleFeedRepository.java`

**`ArticleController`:** Add `level` (`DifficultyLevel`, default `EASY`) and `category` (`String`, optional) `@RequestParam`s to the `GET /api/articles` handler. Pass both to `ArticleFeedService.getFeed()`.

**`ArticleFeedService`:** Accept `level` and `category` in `getFeed()`. Pass them through to repository queries. After the repository returns raw rows, compute `excerpt` (truncated content) and `estimatedReadingTime` (word count) from the joined content field.

**`ArticleFeedRepository`:** Revise both JPQL queries (first page and cursor page) to:
- `JOIN` `article_contents ac ON ac.article_id = a.id AND ac.level = :level`
- Optionally filter `JOIN a.category c WHERE c.displayName = :categoryName` when `category` is not `ALL` and not null
- Return `ac.content` (or a trimmed prefix) alongside the existing article fields, replacing the hardcoded `0` for reading time with content length data
- Articles without a content record at the requested level are excluded (inner join semantics)

**Why required:** Without the level join the feed always shows the same content regardless of user level. Without category filtering, cursor-based pagination cannot be combined with category filtering — offloading this filter to the client breaks cursor semantics.

**DB schema:** No changes. These are read-path query changes only.

**Forbidden:** Do not modify the generation pipeline, scheduler, worker, LLM client, or any write path.

---

## Unsupported Items

The following items are explicitly out of scope for MVP. Document them here so they are not silently added.

| Item | Policy |
|---|---|
| Backend saved/read/visited persistence | localStorage only (UI_POLICY §11) |
| User authentication | Not implemented |
| Article search | Not implemented |
| Category listing API | Frontend uses hardcoded `['All', 'Tech', 'Science', 'Business', 'Culture']` matching `Category.displayName` values seeded in DB |
| Full article TTS | Forbidden (UI_POLICY §5.3) |
| Quiz TTS | Forbidden (UI_POLICY §12) |
| "Start Quiz" button | Forbidden (UI_POLICY §12) |
| "Check Answer" button | Forbidden (UI_POLICY §12) |
| "Read" label on feed cards | Forbidden (UI_POLICY §2.2, §12) |
| Difficulty badge on feed cards | Forbidden (UI_POLICY §2.2) |
| Published date on feed cards | Forbidden (UI_POLICY §2.2) |
| Toast on feed save | Forbidden (UI_POLICY §6.1) |
| Score, streak, gamification | Forbidden (UI_POLICY §0) |
| Separate quiz screen | Forbidden (UI_POLICY §7) |
| Progress bar / percent in continue reading | Forbidden (UI_POLICY §3.3) |
| Summary / date / difficulty on saved screen | Forbidden (UI_POLICY §8.3) |
