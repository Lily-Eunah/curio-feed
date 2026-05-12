import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ArticleBody from '../ArticleBody';
import type { VocabEntry } from '../../../types';

describe('ArticleBody', () => {
  const vocabulary: VocabEntry[] = [
    { word: 'institution', definition: 'A large organization', example: 'The academic institution...' },
    { word: 'disrupt', definition: 'To interrupt', example: 'It might disrupt...' },
  ];

  const onVocabTap = vi.fn();
  const bodyRef = { current: null };

  it('highlights {{word}} tokens correctly', () => {
    const body = 'This is an {{institution}}.';
    render(<ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />);
    
    const word = screen.getByText('institution');
    expect(word).toBeDefined();
    fireEvent.click(word);
    expect(onVocabTap).toHaveBeenCalledWith(vocabulary[0]);
  });

  it('highlights standalone vocabulary words automatically (case-insensitive)', () => {
    const body = 'The Institution was founded long ago.';
    render(<ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />);
    
    const word = screen.getByText('Institution');
    expect(word).toBeDefined();
    fireEvent.click(word);
    expect(onVocabTap).toHaveBeenCalledWith(vocabulary[0]);
  });

  it('highlights inflected forms (plurals, etc.)', () => {
    const body = 'Many institutions were involved. They disrupted everything.';
    render(<ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />);
    
    const w1 = screen.getByText('institutions');
    const w2 = screen.getByText('disrupted');
    
    expect(w1).toBeDefined();
    expect(w2).toBeDefined();
    
    fireEvent.click(w1);
    expect(onVocabTap).toHaveBeenLastCalledWith(vocabulary[0]);
    
    fireEvent.click(w2);
    expect(onVocabTap).toHaveBeenLastCalledWith(vocabulary[1]);
  });

  it('does not highlight parts of other words', () => {
    const body = 'Constitutions are different from institutions.';
    render(<ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />);

    // 'Constitutions' should NOT be highlighted as 'institution' + 's'
    const word = screen.queryByText('Constitutions');
    expect(word).toBeNull();

    const match = screen.getByText('institutions');
    expect(match).toBeDefined();
  });

  it('strips {{word}} markers and renders the word without braces', () => {
    const body = 'Quantum uses {{institution}} for research.';
    render(<ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />);
    expect(screen.getByText('institution')).toBeDefined();
    expect(screen.queryByText('{{institution}}')).toBeNull();
  });

  it('splits paragraphs on actual newlines', () => {
    const body = 'First paragraph.\n\nSecond paragraph.';
    const { container } = render(
      <ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />,
    );
    const paragraphs = container.querySelectorAll('p');
    expect(paragraphs.length).toBe(2);
  });

  it('splits paragraphs on literal backslash-n sequences (legacy seed format)', () => {
    // Simulates content stored in DB with literal \n (backslash+n, not actual newline)
    const body = 'First paragraph.\\n\\nSecond paragraph.';
    const { container } = render(
      <ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />,
    );
    const paragraphs = container.querySelectorAll('p');
    expect(paragraphs.length).toBe(2);
  });

  it('does not render literal backslash-n in paragraph text', () => {
    const body = 'Quantum uses {{institution}}.\\n\\nIt solves problems.';
    render(<ArticleBody body={body} vocabulary={vocabulary} onVocabTap={onVocabTap} bodyRef={bodyRef} />);
    const fullText = document.body.textContent ?? '';
    expect(fullText).not.toContain('\\n');
    expect(fullText).not.toContain('{{');
  });
});
