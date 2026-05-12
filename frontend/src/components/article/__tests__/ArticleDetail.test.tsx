import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
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
    userLevel: 'MEDIUM' as const,
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

  beforeEach(() => {
    vi.clearAllMocks();
  });

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

// ── Next article button visibility ─────────────────────────────────────────────

describe('Next article button', () => {
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

  const nextArticle: Article = {
    ...article,
    id: 'art-2',
    title: 'Following Article',
  };

  const baseProps = {
    article,
    userLevel: 'MEDIUM' as const,
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

  const withQuizAnswered = {
    'art-1:MEDIUM': {
      q1: { status: 'done' as const, correct: true, userAnswer: 0, attempts: 1 },
    },
  };

  it('renders next article card when nextArticle exists and at least one quiz is answered', () => {
    render(
      <ArticleDetail
        {...baseProps}
        nextArticle={nextArticle}
        quizProgress={withQuizAnswered}
      />,
    );
    expect(screen.getByText('Following Article')).toBeDefined();
    expect(screen.getByText('Next Article')).toBeDefined();
  });

  it('does not render next article card when nextArticle is null (last article)', () => {
    render(
      <ArticleDetail
        {...baseProps}
        nextArticle={null}
        quizProgress={withQuizAnswered}
      />,
    );
    expect(screen.queryByText('Next Article')).toBeNull();
  });

  it('does not render next article card when no quiz has been answered yet', () => {
    render(
      <ArticleDetail
        {...baseProps}
        nextArticle={nextArticle}
        quizProgress={{}}
      />,
    );
    expect(screen.queryByText('Next Article')).toBeNull();
  });

  it('calls onNextArticle with the next article id when card is tapped', () => {
    const onNextArticle = vi.fn();
    render(
      <ArticleDetail
        {...baseProps}
        nextArticle={nextArticle}
        quizProgress={withQuizAnswered}
        onNextArticle={onNextArticle}
      />,
    );
    fireEvent.click(screen.getByText('Following Article'));
    expect(onNextArticle).toHaveBeenCalledWith('art-2');
  });

  it('next article card remains visible while isLoading is true (feed refresh guard)', () => {
    render(
      <ArticleDetail
        {...baseProps}
        nextArticle={nextArticle}
        quizProgress={withQuizAnswered}
        isLoading={true}
      />,
    );
    // Loading overlay does not hide the next article card (it is outside the overlay div)
    expect(screen.getByText('Following Article')).toBeDefined();
  });
});

// ── Level switch: article shell maintenance ────────────────────────────────────

describe('Level switch article shell', () => {
  const article: Article = {
    id: 'art-1',
    category: 'Tech',
    date: '2026-05-10',
    readTime: '5 min',
    title: 'Shell Article',
    excerpt: 'Excerpt',
    body: 'Original body content',
    vocabulary: [],
    quiz: {
      q1: { question: 'Q1?', options: ['A', 'B'], correct: 0, explanation: 'Exp' },
      q2: { question: 'Q2?', options: ['C', 'D'], correct: 1, explanation: 'Exp' },
      q3: { question: 'Q3?', modelAnswer: 'Model' },
    },
  };

  const baseProps = {
    article,
    userLevel: 'MEDIUM' as const,
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

  it('shows loading overlay while level switch is in progress', () => {
    render(<ArticleDetail {...baseProps} isLoading={true} />);
    expect(screen.getByText(/Switching to/)).toBeDefined();
    // Title still in DOM behind overlay
    expect(screen.getByText('Shell Article')).toBeDefined();
  });

  it('restores article shell when level switch fails (isLoading returns false, article unchanged)', () => {
    const { rerender } = render(<ArticleDetail {...baseProps} isLoading={true} />);
    expect(screen.getByText(/Switching to/)).toBeDefined();

    // Simulate App.tsx reverting: isLoading=false, article prop unchanged (fetch failed)
    rerender(<ArticleDetail {...baseProps} isLoading={false} />);

    // Loading overlay gone
    expect(screen.queryByText(/Switching to/)).toBeNull();
    // Article title and body still visible
    expect(screen.getByText('Shell Article')).toBeDefined();
    expect(screen.getByText('Original body content')).toBeDefined();
  });
});
