import { useState, memo } from 'react';
import { COLORS } from '../../../theme';
import type { MCQResult } from '../../../types';

interface Props {
  question: string;
  options: string[];
  correct: number;
  explanation: string;
  savedProgress: MCQResult | undefined;
  onAnswer: (result: MCQResult) => void;
}

type Phase = 'idle' | 'wrong-flash' | 'retry' | 'done';

// MCQ retry logic (UI_POLICY §7.1): max 2 attempts
export default memo(function MCQ({ question, options, correct, explanation, savedProgress, onAnswer }: Props) {
  const [phase, setPhase] = useState<Phase>(savedProgress ? 'done' : 'idle');
  const [firstSel, setFirstSel] = useState<number | null>(null);
  const [finalSel, setFinalSel] = useState<number | null>(savedProgress?.userAnswer ?? null);
  const [isCorrect, setIsCorrect] = useState<boolean | null>(savedProgress?.correct ?? null);

  const handleSelect = (idx: number) => {
    if (phase === 'done' || phase === 'wrong-flash') return;

    if (phase === 'idle') {
      if (idx === correct) {
        setFinalSel(idx);
        setIsCorrect(true);
        setPhase('done');
        onAnswer({ status: 'done', correct: true, userAnswer: idx, attempts: 1 });
      } else {
        setFirstSel(idx);
        setPhase('wrong-flash');
        setTimeout(() => setPhase('retry'), 600);
      }
    } else if (phase === 'retry') {
      const wasCorrect = idx === correct;
      setFinalSel(idx);
      setIsCorrect(wasCorrect);
      setPhase('done');
      onAnswer({ status: 'done', correct: wasCorrect, userAnswer: idx, attempts: 2 });
    }
  };

  const optionStyle = (idx: number): React.CSSProperties => {
    let background: string = COLORS.bg;
    let borderColor: string = COLORS.border;
    let color: string = COLORS.text;
    let opacity = 1;
    let cursor: React.CSSProperties['cursor'] = 'pointer';

    if (phase === 'wrong-flash') {
      if (idx === firstSel) {
        background = 'oklch(0.94 0.05 28)';
        borderColor = COLORS.hard;
        color = COLORS.hard;
      }
      cursor = 'default';
    } else if (phase === 'retry') {
      if (idx === firstSel) { opacity = 0.35; cursor = 'default'; }
    } else if (phase === 'done') {
      cursor = 'default';
      if (idx === correct) {
        background = 'oklch(0.95 0.04 145)';
        borderColor = COLORS.easy;
        color = COLORS.easy;
      } else if (idx === finalSel && idx !== correct) {
        background = 'oklch(0.94 0.05 28)';
        borderColor = COLORS.hard;
        color = COLORS.hard;
      } else if (idx === firstSel && firstSel !== finalSel) {
        opacity = 0.3;
      } else {
        opacity = 0.4;
      }
    }

    return { background, borderColor, color, opacity, cursor };
  };

  const optionIcon = (idx: number): React.ReactNode => {
    if (phase !== 'done') return null;
    if (idx === correct) return <span style={{ fontWeight: 700, fontSize: 13 }}>✓</span>;
    if (idx === finalSel && idx !== correct) return <span style={{ fontWeight: 700, fontSize: 13 }}>✗</span>;
    return null;
  };

  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{ fontSize: 15, fontWeight: 600, color: COLORS.text, lineHeight: 1.45, marginBottom: 12 }}>
        {question}
      </div>

      {options.map((opt, i) => {
        const s = optionStyle(i);
        return (
          <button
            key={i}
            onClick={() => handleSelect(i)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              width: '100%',
              textAlign: 'left',
              background: s.background,
              border: `1.5px solid ${s.borderColor}`,
              borderRadius: 10,
              padding: '11px 14px',
              marginBottom: 6,
              cursor: s.cursor,
              fontSize: 14,
              color: s.color,
              lineHeight: 1.4,
              opacity: s.opacity,
              transition: 'background 0.15s ease, border-color 0.15s ease, opacity 0.2s ease',
            }}
          >
            {opt}
            {optionIcon(i)}
          </button>
        );
      })}

      {/* "Not quite. Try again." (UI_POLICY §7.1) */}
      {phase === 'retry' && (
        <div style={{ fontSize: 13, color: COLORS.medium, fontWeight: 500, marginTop: 4, marginBottom: 4, paddingLeft: 2 }}>
          Not quite. Try again.
        </div>
      )}

      {/* Correct answer label when wrong twice (UI_POLICY §7.1) */}
      {phase === 'done' && !isCorrect && (
        <div style={{ fontSize: 12, color: COLORS.textSec, marginTop: 4, marginBottom: 4, paddingLeft: 2 }}>
          The correct answer is <strong style={{ color: COLORS.easy }}>{String.fromCharCode(65 + correct)}</strong>
        </div>
      )}

      {/* Explanation shown after completion */}
      {phase === 'done' && (
        <div
          style={{
            background: COLORS.bg,
            border: `1px solid ${COLORS.borderLight}`,
            borderRadius: 10,
            padding: '10px 14px',
            marginTop: 4,
            fontSize: 13,
            color: COLORS.textSec,
            lineHeight: 1.5,
          }}
        >
          {explanation}
        </div>
      )}
    </div>
  );
});
