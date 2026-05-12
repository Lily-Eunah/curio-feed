import { memo } from 'react';
import { COLORS } from '../../theme';
import type { VocabEntry } from '../../types';

interface Props {
  body: string;
  vocabulary: VocabEntry[];
  onVocabTap: (entry: VocabEntry) => void;
  bodyRef: React.RefObject<HTMLDivElement>;
}

// Parse body text: split paragraphs on \n\n, highlight vocabulary words and {{word}} tokens
function parseBody(
  text: string,
  vocabulary: VocabEntry[],
  onVocabTap: (entry: VocabEntry) => void,
): React.ReactNode[] {
  const vocabMap = new Map(vocabulary.map(v => [v.word.toLowerCase(), v]));
  
  // Sort by length descending to match longer phrases first
  const sortedWords = [...vocabulary].sort((a, b) => b.word.length - a.word.length);
  
  // Create regex pattern that matches {{word}} OR standalone vocab words
  // Inflection: match word + optional (s, es, d, ed, ing, ion, ions) - basic approach
  const patterns = sortedWords.map(v => {
    const escaped = v.word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    // Match exact word OR word with simple inflections
    // We use word boundaries \b to avoid matching parts of other words
    return `\\b${escaped}(?:s|es|d|ed|ing|ion|ions)?\\b`;
  });

  // Combine with the {{}} pattern
  const combinedPattern = new RegExp(`({{[^}]+}})|(${patterns.join('|')})`, 'gi');

  // Normalize literal \n (backslash + n stored in legacy seed SQL) to actual newlines
  const normalized = text.replace(/\\n/g, '\n');

  return normalized.split('\n\n').map((para, pIdx) => {
    const parts = para.split(combinedPattern);
    const children = parts.filter(p => p !== undefined && p !== '').map((part, i) => {
      const markerMatch = part.match(/^{{(.+)}}$/);
      if (markerMatch) {
        const word = markerMatch[1];
        const entry = vocabMap.get(word.toLowerCase());
        return (
          <span
            key={i}
            onClick={() => entry && onVocabTap(entry)}
            style={{
              color: COLORS.accent,
              borderBottom: `1.5px dotted ${COLORS.accentMid}`,
              cursor: 'pointer',
              paddingBottom: 1,
            }}
          >
            {word}
          </span>
        );
      }

      // Check if it's a standalone vocab match
      const lowercasePart = part.toLowerCase();
      let entry = vocabMap.get(lowercasePart);
      
      // If not exact match, try stripping inflections to find the base entry
      if (!entry) {
        const base = sortedWords.find(v => {
          const w = v.word.toLowerCase();
          return lowercasePart === w + 's' || 
                 lowercasePart === w + 'es' || 
                 lowercasePart === w + 'd' || 
                 lowercasePart === w + 'ed' || 
                 lowercasePart === w + 'ing' ||
                 lowercasePart === w + 'ion' ||
                 lowercasePart === w + 'ions';
        });
        if (base) entry = base;
      }

      if (entry) {
        return (
          <span
            key={i}
            onClick={() => entry && onVocabTap(entry)}
            style={{
              color: COLORS.accent,
              borderBottom: `1.5px dotted ${COLORS.accentMid}`,
              cursor: 'pointer',
              paddingBottom: 1,
            }}
          >
            {part}
          </span>
        );
      }

      return part;
    });

    return (
      <p key={pIdx} style={{ marginBottom: '1.35em', lineHeight: 1.75 }}>
        {children}
      </p>
    );
  });
}

export default memo(function ArticleBody({ body, vocabulary, onVocabTap, bodyRef }: Props) {
  return (
    <div
      ref={bodyRef}
      style={{
        fontFamily: 'Lora, Georgia, serif',
        fontSize: 17,
        color: COLORS.text,
      }}
    >
      {parseBody(body, vocabulary, onVocabTap)}
    </div>
  );
});
