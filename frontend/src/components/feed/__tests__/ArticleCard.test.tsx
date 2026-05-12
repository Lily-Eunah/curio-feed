import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ArticleCard from '../ArticleCard';
import type { Article } from '../../../types';

describe('ArticleCard', () => {
  const article: Article = {
    id: 'art-1',
    category: 'Tech',
    date: 'May 10, 2026',
    readTime: '5 min',
    title: 'Quantum Computing',
    excerpt: 'Quantum uses {{qubits}}.\\n\\nIt solves {{ complex }} problems.',
    vocabulary: [],
    body: '',
    quiz: {
      q1: { question: '', options: [], correct: 0, explanation: '' },
      q2: { question: '', options: [], correct: 0, explanation: '' },
      q3: { question: '', modelAnswer: '' },
    },
  };

  it('sanitizes excerpt correctly in the UI', () => {
    render(
      <ArticleCard
        article={article}
        isVisited={false}
        isSaved={false}
        onTap={() => {}}
        onSave={() => {}}
      />
    );

    // Should NOT find markers or literal \n
    const element = screen.getByText(/Quantum uses qubits/);
    expect(element).toBeDefined();
    expect(element.textContent).toContain('Quantum uses qubits. It solves complex problems.');
    expect(element.textContent).not.toContain('{{');
    expect(element.textContent).not.toContain('\\n');
  });
});
