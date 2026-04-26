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
- `excerpt`: short preview string (~160 characters). Derived server-side from the first paragraph of the article's `EASY` content. Not stored in DB. May be truncated with `…` if over limit.
- `thumbnailUrl`: may be empty string `""`. Frontend must handle gracefully.
- `sourceName`: publication name string.

**Frontend display rules** (UI_POLICY §2.2):

Feed cards show: category dot, category label, title, excerpt, reading time.

Feed cards do NOT show: difficulty badge, published date, "5 words", "Read" label.

**Backend implementation notes:**

- `ArticleFeedRepository` currently hardcodes `estimatedReadingTime = 0` in JPQL constructor queries — this must be fixed in the service layer or by changing the JPQL to a result-set projection.
- `excerpt` is not a DB column. The service must derive it by loading the article's EASY content on feed fetch or by deriving from `originalContent`. See [Required Backend DTO Additions](#required-backend-dto-additions).

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

**Status:** Not yet implemented. Will be added if required by the frontend integration; otherwise the MVP fallback below applies.

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

**UI rule (UI_POLICY §10):** Next article is shown after ≥1 quiz answered. Same category, same level, older article. No "next article" button visible before any quiz is answered.

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

`excerpt` is NOT a DB column. It is computed in the service layer:
- Derive from `Article.originalContent`: take the first sentence or first ~160 characters, strip trailing whitespace, append `…` if truncated.
- If `originalContent` is null or blank, fall back to empty string `""`.

`estimatedReadingTime` is currently hardcoded to `0` in the JPQL constructor queries in `ArticleFeedRepository`. Fix by computing in the service layer after the query returns: for each result, derive reading time from available content if needed, or use a fixed estimate of `5` as a safe default until content-based computation is implemented.

**Why required:** Feed cards must display excerpt (UI_POLICY §2.2) and reading time. Hardcoded 0 produces "0 min read" which is incorrect product behavior.

**DB schema:** No changes. `excerpt` is not persisted. `estimatedReadingTime` is not persisted.

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
