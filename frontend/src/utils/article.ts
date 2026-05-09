/**
 * Mapping utilities — convert backend DTOs to frontend display models.
 */

import type { QuizDto, ArticleDetailDto, FeedArticleDto } from '../api/types';
import type { Article } from '../types';

// ── Mapped types ────────────────────────────────────────────────────────────────

interface MappedMCQ {
  question: string;
  options: string[];
}

interface MappedSA {
  question: string;
  modelAnswer: string;
}

interface MappedQuizzes {
  q1: MappedMCQ;
  q2: MappedMCQ;
  q3: MappedSA;
}

interface MappedVocab {
  word: string;
  definition: string;
  example: string;
}

interface MappedArticle {
  id: string;
  title: string;
  category: string;
  vocabulary: MappedVocab[];
  body: string;
  readTime: string;
}

// ── Placeholders ────────────────────────────────────────────────────────────────

const PLACEHOLDER_MCQ: MappedMCQ = {
  question: 'Quiz not available',
  options: ['A', 'B', 'C', 'D'],
};

const PLACEHOLDER_SA: MappedSA = {
  question: 'Quiz not available',
  modelAnswer: '',
};

// ── Reading time ─────────────────────────────────────────────────────────────────

const LEVEL_WPM: Record<string, number> = {
  EASY: 90,
  MEDIUM: 120,
  HARD: 140,
};
const DEFAULT_WPM = 120;

/** Returns "X min read" — rounded up to whole minutes, minimum 1. */
function calcReadingTime(wordCount: number, level?: string): string {
  const wpm = (level ? LEVEL_WPM[level] : undefined) ?? DEFAULT_WPM;
  const minutes = Math.max(1, Math.ceil(wordCount / wpm));
  return `${minutes} min read`;
}

// ── Public API ──────────────────────────────────────────────────────────────────

export function mapBackendQuizzes(quizzes: QuizDto[]): MappedQuizzes {
  const mcqs = quizzes.filter((q) => q.type === 'MULTIPLE_CHOICE');
  const sas = quizzes.filter((q) => q.type === 'SHORT_ANSWER');

  const q1: MappedMCQ = mcqs[0]
    ? { question: mcqs[0].question, options: mcqs[0].options.choices?.map((c) => c.text) ?? [] }
    : { ...PLACEHOLDER_MCQ };

  const q2: MappedMCQ = mcqs[1]
    ? { question: mcqs[1].question, options: mcqs[1].options.choices?.map((c) => c.text) ?? [] }
    : { ...PLACEHOLDER_MCQ };

  const q3: MappedSA = sas[0]
    ? {
        question: sas[0].question,
        modelAnswer: (sas[0].options.explanations as Record<string, string> | null)?.modelAnswer ?? '',
      }
    : { ...PLACEHOLDER_SA };

  return { q1, q2, q3 };
}

export function mapDetailDtoToArticle(dto: ArticleDetailDto): MappedArticle {
  const wordCount = dto.content.content.split(/\s+/).filter(Boolean).length;

  return {
    id: dto.id,
    title: dto.title,
    category: dto.categoryName,
    vocabulary: dto.content.vocabularies.map((v) => ({
      word: v.word,
      definition: v.definition,
      example: v.exampleSentence,
    })),
    body: dto.content.content,
    readTime: calcReadingTime(wordCount, dto.content.level),
  };
}

// ── App-level mapping (returns full Article type for Feed/ArticleDetail components) ──

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
  });
}

const PLACEHOLDER_QUIZ: Article['quiz'] = {
  q1: { question: 'Loading…', options: ['…', '…', '…', '…'], correct: 0, explanation: '' },
  q2: { question: 'Loading…', options: ['…', '…', '…', '…'], correct: 0, explanation: '' },
  q3: { question: 'Loading…', modelAnswer: '…' },
};

export function mapFeedArticle(dto: FeedArticleDto): Article {
  return {
    id: dto.id,
    category: dto.categoryName,
    date: formatDate(dto.publishedAt),
    readTime: `${dto.estimatedReadingTime} min`,
    title: dto.title,
    excerpt: dto.excerpt,
    vocabulary: [],
    body: '',
    quiz: PLACEHOLDER_QUIZ,
  };
}

export function mapFullArticle(dto: ArticleDetailDto, feedArticle?: Article): Article {
  const wordCount = dto.content.content.split(/\s+/).filter(Boolean).length;
  const mcqs = dto.content.quizzes.filter((q) => q.type === 'MULTIPLE_CHOICE');
  const sas = dto.content.quizzes.filter((q) => q.type === 'SHORT_ANSWER');

  const q1 = mcqs[0]
    ? {
        question: mcqs[0].question,
        options: mcqs[0].options.choices?.map((c) => c.text) ?? [],
        correct: parseInt(mcqs[0].correctAnswer ?? '0', 10),
        explanation: mcqs[0].explanation ?? '',
      }
    : PLACEHOLDER_QUIZ.q1;

  const q2 = mcqs[1]
    ? {
        question: mcqs[1].question,
        options: mcqs[1].options.choices?.map((c) => c.text) ?? [],
        correct: parseInt(mcqs[1].correctAnswer ?? '0', 10),
        explanation: mcqs[1].explanation ?? '',
      }
    : { ...q1 };

  const q3 = sas[0]
    ? { question: sas[0].question, modelAnswer: sas[0].correctAnswer ?? '' }
    : PLACEHOLDER_QUIZ.q3;

  return {
    id: dto.id,
    category: dto.categoryName,
    date: feedArticle?.date ?? formatDate(dto.publishedAt),
    readTime: feedArticle?.readTime ?? calcReadingTime(wordCount, dto.content.level),
    title: dto.title,
    excerpt: feedArticle?.excerpt ?? '',
    vocabulary: dto.content.vocabularies.map((v) => ({
      word: v.word,
      definition: v.definition,
      example: v.exampleSentence,
    })),
    body: dto.content.content,
    quiz: { q1, q2, q3 },
  };
}
