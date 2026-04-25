import { memo } from 'react';
import { COLORS } from '../../theme';
import type { VocabEntry } from '../../types';

interface Props {
  body: string;
  vocabulary: VocabEntry[];
  onVocabTap: (entry: VocabEntry) => void;
  bodyRef: React.RefObject<HTMLDivElement>;
}

// Parse body text: split paragraphs on \n\n, highlight {{word}} tokens
function parseBody(
  text: string,
  vocabMap: Map<string, VocabEntry>,
  onVocabTap: (entry: VocabEntry) => void,
): React.ReactNode[] {
  return text.split('\n\n').map((para, pIdx) => {
    const parts = para.split(/({{[^}]+}})/);
    const children = parts.map((part, i) => {
      const match = part.match(/^{{(.+)}}$/);
      if (match) {
        const word = match[1];
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
  const vocabMap = new Map(vocabulary.map(v => [v.word.toLowerCase(), v]));

  return (
    <div
      ref={bodyRef}
      style={{
        fontFamily: 'Lora, Georgia, serif',
        fontSize: 17,
        color: COLORS.text,
      }}
    >
      {parseBody(body, vocabMap, onVocabTap)}
    </div>
  );
});
