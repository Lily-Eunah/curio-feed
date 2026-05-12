import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ArticleDetail from '../ArticleDetail';
import type { Article } from '../../../types';

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
    quizProgress: {},
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

  it('preserves metadata shell when isLoading is true', () => {
    render(
      <ArticleDetail {...mockProps} userLevel="EASY" isLoading={true} />
    );

    // Title and Meta should be visible
    expect(screen.getByText('Test Article')).toBeDefined();
    expect(screen.getByText('TECH')).toBeDefined();
    expect(screen.getByText('5 min')).toBeDefined();

    // Loading indicator should be present
    expect(screen.getByText(/Switching to/)).toBeDefined();
  });

  it('disables Easier button when EASY is not in availableLevels', () => {
    const articleMediumOnly: Article = {
      ...article,
      availableLevels: ['MEDIUM'],
    };
    render(
      <ArticleDetail {...mockProps} article={articleMediumOnly} userLevel="MEDIUM" />,
    );
    const easierBtn = screen.getByText(/Easier/);
    // Button should be rendered but non-interactive (cursor: default, disabled styling)
    expect(easierBtn).toBeDefined();
    // Clicking should NOT call onLevelChange
    fireEvent.click(easierBtn);
    expect(mockProps.onLevelChange).not.toHaveBeenCalled();
  });

  it('disables Harder button when HARD is not in availableLevels', () => {
    const articleMediumOnly: Article = {
      ...article,
      availableLevels: ['MEDIUM'],
    };
    render(
      <ArticleDetail {...mockProps} article={articleMediumOnly} userLevel="MEDIUM" />,
    );
    const harderBtn = screen.getByText(/Harder/);
    expect(harderBtn).toBeDefined();
    fireEvent.click(harderBtn);
    expect(mockProps.onLevelChange).not.toHaveBeenCalled();
  });

  it('calls onLevelChange when switching to an available level', () => {
    const articleAllLevels: Article = {
      ...article,
      availableLevels: ['EASY', 'MEDIUM', 'HARD'],
    };
    const onLevelChange = vi.fn();
    render(
      <ArticleDetail
        {...mockProps}
        article={articleAllLevels}
        userLevel="MEDIUM"
        onLevelChange={onLevelChange}
      />,
    );
    const harderBtn = screen.getByText(/Harder/);
    fireEvent.click(harderBtn);
    expect(onLevelChange).toHaveBeenCalledWith('HARD');
  });
});
