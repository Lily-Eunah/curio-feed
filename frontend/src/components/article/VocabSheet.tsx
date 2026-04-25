import { memo, useCallback } from 'react';
import BottomSheet from '../ui/BottomSheet';
import { COLORS } from '../../theme';
import type { VocabEntry } from '../../types';

interface Props {
  vocab: VocabEntry | null;
  onClose: () => void;
}

// TTS: word pronunciation only (UI_POLICY §5.3)
function speak(word: string) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(word);
  window.speechSynthesis.speak(utterance);
}

export default memo(function VocabSheet({ vocab, onClose }: Props) {
  const handleSpeak = useCallback(() => {
    if (vocab) speak(vocab.word);
  }, [vocab]);

  return (
    <BottomSheet open={!!vocab} onClose={onClose}>
      {vocab && (
        <div style={{ padding: '8px 20px 32px' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 4 }}>
            <div style={{ fontSize: 24, fontWeight: 700, color: COLORS.text, fontFamily: 'Lora, serif' }}>
              {vocab.word}
            </div>
            {/* TTS button — only for the word (UI_POLICY §5.3) */}
            <button
              onClick={handleSpeak}
              style={{
                background: COLORS.accentLight,
                border: 'none',
                borderRadius: 8,
                padding: '6px 8px',
                cursor: 'pointer',
                color: COLORS.accent,
                display: 'flex',
                alignItems: 'center',
                gap: 4,
                fontSize: 12,
                fontWeight: 500,
              }}
              aria-label={`Pronounce ${vocab.word}`}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
                <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
                <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
              </svg>
              Listen
            </button>
          </div>
          <div style={{ fontSize: 14, color: COLORS.textSec, lineHeight: 1.6, marginBottom: 16 }}>
            {vocab.definition}
          </div>
          <div
            style={{
              background: COLORS.bg,
              borderRadius: 10,
              padding: '12px 14px',
              fontSize: 14,
              color: COLORS.textSec,
              fontStyle: 'italic',
              fontFamily: 'Lora, serif',
              lineHeight: 1.6,
            }}
          >
            "{vocab.example}"
          </div>
        </div>
      )}
    </BottomSheet>
  );
});
