import { memo, useCallback } from 'react';
import { COLORS } from '../../theme';
import type { VocabEntry } from '../../types';

interface Props {
  vocabulary: VocabEntry[];
}

// TTS: word pronunciation only (UI_POLICY §5.4)
function speak(word: string) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(word);
  window.speechSynthesis.speak(utterance);
}

function VocabItem({ entry, isLast }: { entry: VocabEntry; isLast: boolean }) {
  const handleSpeak = useCallback(() => speak(entry.word), [entry.word]);

  return (
    <div
      style={{
        padding: '12px 0',
        borderBottom: isLast ? 'none' : `1px solid ${COLORS.borderLight}`,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 3 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: COLORS.accent, fontFamily: 'Lora, serif' }}>
          {entry.word}
        </div>
        <button
          onClick={handleSpeak}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: COLORS.textTer,
            padding: 4,
            display: 'flex',
            alignItems: 'center',
          }}
          aria-label={`Pronounce ${entry.word}`}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
            <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
          </svg>
        </button>
      </div>
      <div style={{ fontSize: 13, color: COLORS.textSec, lineHeight: 1.5, marginBottom: 4 }}>
        {entry.definition}
      </div>
      <div style={{ fontSize: 12, color: COLORS.textTer, fontStyle: 'italic', fontFamily: 'Lora, serif', lineHeight: 1.5 }}>
        "{entry.example}"
      </div>
    </div>
  );
}

export default memo(function VocabReview({ vocabulary }: Props) {
  return (
    <div>
      <div
        style={{
          fontSize: 13,
          fontWeight: 700,
          color: COLORS.textTer,
          letterSpacing: '0.07em',
          textTransform: 'uppercase',
          marginBottom: 14,
        }}
      >
        Vocabulary Review
      </div>
      {vocabulary.map((v, i) => (
        <VocabItem key={v.word} entry={v} isLast={i === vocabulary.length - 1} />
      ))}
    </div>
  );
});
