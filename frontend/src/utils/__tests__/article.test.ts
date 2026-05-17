import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mapBackendQuizzes, mapDetailDtoToArticle, mapFullArticle, resolveAvailableLevel, fetchDetailWithFallback } from '../article';
import { fetchArticleDetail, ApiError } from '../../api/client';
import type { ArticleDetailDto, QuizDto, VocabularyDto } from '../../api/types';

vi.mock('../../api/client', () => {
  class MockApiError extends Error {
    status: number;
    body: { error: string; message: string };
    constructor(status: number, body: { error: string; message: string }) {
      super(body.message);
      this.status = status;
      this.body = body;
      this.name = 'ApiError';
    }
  }
  return { fetchArticleDetail: vi.fn(), ApiError: MockApiError };
});

const mockFetch = vi.mocked(fetchArticleDetail);

// ── Helpers ──────────────────────────────────────────────────────────────────

function makeQuizDto(type: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER', question: string, overrides: Partial<QuizDto> = {}): QuizDto {
  return {
    id: 'q-' + Math.random().toString(36).slice(2),
    type,
    question,
    options: type === 'MULTIPLE_CHOICE'
      ? {
          choices: [
            { key: 'A', text: 'Option A', explanation: null },
            { key: 'B', text: 'Option B', explanation: null },
            { key: 'C', text: 'Option C', explanation: null },
          ],
          explanations: null,
        }
      : { choices: null, explanations: { modelAnswer: 'The model answer.' } },
    ...overrides,
  };
}

function makeDetailDto(overrides: Partial<ArticleDetailDto> = {}): ArticleDetailDto {
  const vocabs: VocabularyDto[] = [
    { word: 'ambient', definition: 'Present everywhere.', exampleSentence: 'Ambient music.' },
  ];
  return {
    id: 'a1',
    title: 'Test Article',
    originalTitle: 'Original Title',
    sourceName: 'Test Source',
    sourceUrl: 'https://example.com',
    publishedAt: '2026-04-26T08:00:00Z',
    categoryName: 'Tech',
    availableLevels: ['EASY', 'MEDIUM'],
    content: {
      id: 'c1',
      level: 'EASY',
      content: 'The article body text.\n\nSecond paragraph.',
      audioUrl: null,
      vocabularies: vocabs,
      quizzes: [
        makeQuizDto('MULTIPLE_CHOICE', 'What is the main idea?'),
        makeQuizDto('MULTIPLE_CHOICE', 'Which word fits here?'),
        makeQuizDto('SHORT_ANSWER', 'Explain in your own words.'),
      ],
    },
    ...overrides,
  };
}

// ── mapBackendQuizzes ─────────────────────────────────────────────────────────

describe('mapBackendQuizzes', () => {
  it('maps first MULTIPLE_CHOICE quiz to q1', () => {
    const quizzes = [
      makeQuizDto('MULTIPLE_CHOICE', 'Q1 question'),
      makeQuizDto('MULTIPLE_CHOICE', 'Q2 question'),
      makeQuizDto('SHORT_ANSWER', 'Q3 question'),
    ];
    const result = mapBackendQuizzes(quizzes);
    expect(result.q1.question).toBe('Q1 question');
  });

  it('maps second MULTIPLE_CHOICE quiz to q2', () => {
    const quizzes = [
      makeQuizDto('MULTIPLE_CHOICE', 'Q1'),
      makeQuizDto('MULTIPLE_CHOICE', 'Q2 question'),
      makeQuizDto('SHORT_ANSWER', 'Q3'),
    ];
    const result = mapBackendQuizzes(quizzes);
    expect(result.q2.question).toBe('Q2 question');
  });

  it('maps first SHORT_ANSWER quiz to q3', () => {
    const quizzes = [
      makeQuizDto('MULTIPLE_CHOICE', 'Q1'),
      makeQuizDto('MULTIPLE_CHOICE', 'Q2'),
      makeQuizDto('SHORT_ANSWER', 'Q3 question'),
    ];
    const result = mapBackendQuizzes(quizzes);
    expect(result.q3.question).toBe('Q3 question');
  });

  it('maps choice texts to options array', () => {
    const quizzes = [makeQuizDto('MULTIPLE_CHOICE', 'Q')];
    const result = mapBackendQuizzes(quizzes);
    expect(result.q1.options).toEqual(['Option A', 'Option B', 'Option C']);
  });

  it('maps modelAnswer from correctAnswer', () => {
    const quizzes = [
      makeQuizDto('MULTIPLE_CHOICE', 'Q1'),
      makeQuizDto('MULTIPLE_CHOICE', 'Q2'),
      makeQuizDto('SHORT_ANSWER', 'Q3', { correctAnswer: 'Expected answer text.' }),
    ];
    const result = mapBackendQuizzes(quizzes);
    expect(result.q3.modelAnswer).toBe('Expected answer text.');
  });

  it('returns placeholder quiz when quizzes list is empty', () => {
    const result = mapBackendQuizzes([]);
    expect(result.q1.question).toBeDefined();
    expect(result.q2.question).toBeDefined();
    expect(result.q3.question).toBeDefined();
  });

  it('returns placeholder MCQ when only SHORT_ANSWER quizzes present', () => {
    const quizzes = [makeQuizDto('SHORT_ANSWER', 'only SA')];
    const result = mapBackendQuizzes(quizzes);
    expect(result.q1.options).toBeDefined();
    expect(Array.isArray(result.q1.options)).toBe(true);
  });
});

// ── mapDetailDtoToArticle ─────────────────────────────────────────────────────

describe('mapDetailDtoToArticle', () => {
  it('maps title', () => {
    const dto = makeDetailDto({ title: 'Mapped Title' });
    const article = mapDetailDtoToArticle(dto);
    expect(article.title).toBe('Mapped Title');
  });

  it('maps categoryName to category', () => {
    const dto = makeDetailDto({ categoryName: 'Science' });
    const article = mapDetailDtoToArticle(dto);
    expect(article.category).toBe('Science');
  });

  it('maps vocabulary exampleSentence to example', () => {
    const dto = makeDetailDto();
    const article = mapDetailDtoToArticle(dto);
    expect(article.vocabulary[0].example).toBe('Ambient music.');
  });

  it('maps vocabulary word and definition', () => {
    const dto = makeDetailDto();
    const article = mapDetailDtoToArticle(dto);
    expect(article.vocabulary[0].word).toBe('ambient');
    expect(article.vocabulary[0].definition).toBe('Present everywhere.');
  });

  it('maps content body as-is', () => {
    const dto = makeDetailDto();
    const article = mapDetailDtoToArticle(dto);
    expect(article.body).toContain('The article body text.');
  });

  it('maps id from dto.id', () => {
    const dto = makeDetailDto({ id: 'uuid-123' });
    const article = mapDetailDtoToArticle(dto);
    expect(article.id).toBe('uuid-123');
  });

  it('includes a non-empty readTime string', () => {
    const dto = makeDetailDto();
    const article = mapDetailDtoToArticle(dto);
    expect(article.readTime).toBeTruthy();
  });
});

// ── resolveAvailableLevel ─────────────────────────────────────────────────────

describe('resolveAvailableLevel', () => {
  it('returns preferred level when it is in availableLevels', () => {
    expect(resolveAvailableLevel('HARD', ['EASY', 'MEDIUM', 'HARD'])).toBe('HARD');
  });

  it('returns MEDIUM when preferred is not available but MEDIUM is', () => {
    expect(resolveAvailableLevel('HARD', ['EASY', 'MEDIUM'])).toBe('MEDIUM');
  });

  it('returns first available level when neither preferred nor MEDIUM exist', () => {
    expect(resolveAvailableLevel('HARD', ['EASY'])).toBe('EASY');
  });

  it('returns preferred when availableLevels is undefined (unknown)', () => {
    expect(resolveAvailableLevel('HARD', undefined)).toBe('HARD');
  });

  it('returns preferred when availableLevels is empty', () => {
    expect(resolveAvailableLevel('MEDIUM', [])).toBe('MEDIUM');
  });

  it('returns preferred when it is the only available level', () => {
    expect(resolveAvailableLevel('EASY', ['EASY'])).toBe('EASY');
  });
});

// ── fetchDetailWithFallback ───────────────────────────────────────────────────

describe('fetchDetailWithFallback', () => {
  const detail404Error = () =>
    new (ApiError as unknown as new (s: number, b: { error: string; message: string }) => InstanceType<typeof ApiError>)(
      404,
      { error: 'Not Found', message: 'Level not available' },
    );

  const sampleDetail = makeDetailDto({ id: 'art-1' });

  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('returns detail directly when fetch succeeds on first try', async () => {
    mockFetch.mockResolvedValue(sampleDetail);
    const result = await fetchDetailWithFallback('art-1', 'HARD');
    expect(result).toBe(sampleDetail);
    expect(mockFetch).toHaveBeenCalledTimes(1);
    expect(mockFetch).toHaveBeenCalledWith('art-1', 'HARD');
  });

  it('falls back to MEDIUM when preferred level returns 404', async () => {
    const mediumDetail = makeDetailDto({ id: 'art-1', content: { ...makeDetailDto().content, level: 'MEDIUM' } });
    mockFetch.mockRejectedValueOnce(detail404Error()).mockResolvedValueOnce(mediumDetail);
    const result = await fetchDetailWithFallback('art-1', 'HARD');
    expect(result).toBe(mediumDetail);
    expect(mockFetch).toHaveBeenCalledTimes(2);
    expect(mockFetch).toHaveBeenNthCalledWith(1, 'art-1', 'HARD');
    expect(mockFetch).toHaveBeenNthCalledWith(2, 'art-1', 'MEDIUM');
  });

  it('throws when both preferred and MEDIUM fallback fail', async () => {
    mockFetch
      .mockRejectedValueOnce(detail404Error())
      .mockRejectedValueOnce(new Error('MEDIUM also unavailable'));
    await expect(fetchDetailWithFallback('art-1', 'HARD')).rejects.toThrow();
    expect(mockFetch).toHaveBeenCalledTimes(2);
  });

  it('does not retry when preferred level is already MEDIUM and gets 404', async () => {
    mockFetch.mockRejectedValue(detail404Error());
    await expect(fetchDetailWithFallback('art-1', 'MEDIUM')).rejects.toThrow();
    expect(mockFetch).toHaveBeenCalledTimes(1);
  });

  it('rethrows non-404 errors without retrying', async () => {
    const networkErr = new Error('Network error');
    mockFetch.mockRejectedValue(networkErr);
    await expect(fetchDetailWithFallback('art-1', 'HARD')).rejects.toBe(networkErr);
    expect(mockFetch).toHaveBeenCalledTimes(1);
  });
});

// ── mapFullArticle — MCQ correct index ────────────────────────────────────────

describe('mapFullArticle MCQ correct index', () => {
  function makeMcqDto(correctAnswer: string): QuizDto {
    return {
      id: 'q1',
      type: 'MULTIPLE_CHOICE',
      question: 'Q?',
      options: {
        choices: [
          { key: 'A', text: 'Alpha', explanation: null },
          { key: 'B', text: 'Beta', explanation: null },
          { key: 'C', text: 'Gamma', explanation: null },
          { key: 'D', text: 'Delta', explanation: null },
        ],
        explanations: null,
      },
      correctAnswer,
      explanation: 'Because.',
    };
  }

  function makeDto(q1Answer: string, q2Answer: string): ArticleDetailDto {
    return makeDetailDto({
      content: {
        ...makeDetailDto().content,
        quizzes: [
          makeMcqDto(q1Answer),
          makeMcqDto(q2Answer),
          makeQuizDto('SHORT_ANSWER', 'Q3', { correctAnswer: 'model answer sentence' }),
        ],
      },
    });
  }

  it('maps correctAnswer "A" to index 0', () => {
    const article = mapFullArticle(makeDto('A', 'B'));
    expect(article.quiz.q1.correct).toBe(0);
  });

  it('maps correctAnswer "B" to index 1', () => {
    const article = mapFullArticle(makeDto('B', 'A'));
    expect(article.quiz.q1.correct).toBe(1);
  });

  it('maps correctAnswer "C" to index 2', () => {
    const article = mapFullArticle(makeDto('C', 'A'));
    expect(article.quiz.q1.correct).toBe(2);
  });

  it('maps correctAnswer "D" to index 3', () => {
    const article = mapFullArticle(makeDto('D', 'A'));
    expect(article.quiz.q1.correct).toBe(3);
  });

  it('correct index is never NaN', () => {
    const article = mapFullArticle(makeDto('B', 'C'));
    expect(Number.isNaN(article.quiz.q1.correct)).toBe(false);
    expect(Number.isNaN(article.quiz.q2.correct)).toBe(false);
  });

  it('maps q3 correctAnswer as modelAnswer', () => {
    const article = mapFullArticle(makeDto('A', 'B'));
    expect(article.quiz.q3.modelAnswer).toBe('model answer sentence');
  });
});
