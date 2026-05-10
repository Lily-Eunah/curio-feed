import { describe, it, expect } from 'vitest';
import { mapBackendQuizzes, mapDetailDtoToArticle } from '../article';
import type { ArticleDetailDto, QuizDto, VocabularyDto } from '../../api/types';

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

  it('maps modelAnswer from explanations', () => {
    const quizzes = [
      makeQuizDto('MULTIPLE_CHOICE', 'Q1'),
      makeQuizDto('MULTIPLE_CHOICE', 'Q2'),
      makeQuizDto('SHORT_ANSWER', 'Q3', {
        options: { choices: null, explanations: { modelAnswer: 'Expected answer text.' } },
      }),
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
