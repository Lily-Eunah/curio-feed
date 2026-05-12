import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ArticleDetail from '../ArticleDetail';
import type { Article, DifficultyLevel } from '../../../types';

describe('ArticleDetail', () => {
  const article: Article = {
    id: 'art-1',
    category: 'Tech',
    date: '2026-05-10',
    readTime: '5 min',
    title: 'Test Article',
    excerpt: 'Excerpt',
    body: 'Body',
    vocabulary: [],
    quiz: {
      q1: { question: 'Q1?', options: ['A', 'B'], correct: 0, explanation: 'Exp' },
      q2: { question: 'Q2?', options: ['C', 'D'], correct: 1, explanation: 'Exp' },
      q3: { question: 'Q3?', modelAnswer: 'Model' },
    },
  };

  const mockProps = {
    article,
    savedIds: [],
    continueReading: null,
    onBack: vi.fn(),
    onSave: vi.fn(),
    onQuizAnswer: vi.fn(),
    onScrollProgress: vi.fn(),
    onLevelChange: vi.fn(),
    nextArticle: null,
    onNextArticle: vi.fn(),
  };

  it('isolates quiz progress by level', () => {
    const quizProgress = {
      'art-1:EASY': {
        q1: { status: 'done', correct: true, userAnswer: 0, attempts: 1 },
      },
      'art-1:MEDIUM': {
        q1: { status: 'done', correct: false, userAnswer: 1, attempts: 2 },
      },
    };

    // Render as EASY
    const { rerender } = render(
      <ArticleDetail {...mockProps} userLevel="EASY" quizProgress={quizProgress} />
    );

    // Should show correct indicator for A (MCQ question 1 index 0)
    expect(screen.getByText('✓')).toBeDefined();
    // In MCQ.tsx, correct answer index 0 gets a checkmark if phase is done.
    
    // Render as MEDIUM
    rerender(
      <ArticleDetail {...mockProps} userLevel="MEDIUM" quizProgress={quizProgress} />
    );

    // In MEDIUM, q1 is wrong (userAnswer 1, correct 0).
    // Should show correct indicator for correct answer (index 0) AND wrong indicator for user answer (index 1)
    expect(screen.getByText('✓')).toBeDefined(); // correct answer indicator
    expect(screen.getByText('✗')).toBeDefined(); // user wrong answer indicator
  });
});
