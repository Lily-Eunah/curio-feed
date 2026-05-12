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
});
