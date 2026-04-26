import { useState, memo } from 'react';
import { COLORS } from '../../../theme';
import type { ShortAnswerResult } from '../../../types';

interface Props {
  question: string;
  modelAnswer: string;
  savedProgress: ShortAnswerResult | undefined;
  onAnswer: (result: ShortAnswerResult) => void;
}

export default memo(function ShortAnswer({ question, modelAnswer, savedProgress, onAnswer }: Props) {
  const [text, setText] = useState(savedProgress?.userText ?? '');
  const [submitted, setSubmitted] = useState(!!savedProgress);
  const [selfEval, setSelfEval] = useState<'yes' | 'no' | null>(savedProgress?.selfEval ?? null);

  const handleSubmit = () => {
    if (!text.trim()) return;
    setSubmitted(true);
  };

  const handleSelfEval = (val: 'yes' | 'no') => {
    setSelfEval(val);
    onAnswer({ status: 'done', selfEval: val, userText: text });
  };

  return (
    <div style={{ marginBottom: 8 }}>
      <div style={{ fontSize: 15, fontWeight: 600, color: COLORS.text, lineHeight: 1.45, marginBottom: 12 }}>
        {question}
      </div>

      {!submitted ? (
        <>
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            placeholder="Write your answer here…"
            rows={4}
            style={{
              width: '100%',
              boxSizing: 'border-box',
              border: `1.5px solid ${COLORS.border}`,
              borderRadius: 10,
              padding: '11px 14px',
              fontSize: 14,
              color: COLORS.text,
              background: COLORS.bg,
              lineHeight: 1.5,
              resize: 'none',
              outline: 'none',
              transition: 'border-color 0.15s ease',
            }}
            onFocus={e => (e.target.style.borderColor = COLORS.accent)}
            onBlur={e => (e.target.style.borderColor = COLORS.border)}
          />
          <button
            onClick={handleSubmit}
            disabled={!text.trim()}
            style={{
              marginTop: 8,
              width: '100%',
              padding: '12px',
              background: text.trim() ? COLORS.text : COLORS.border,
              color: text.trim() ? '#fff' : COLORS.textTer,
              border: 'none',
              borderRadius: 10,
              fontSize: 14,
              fontWeight: 600,
              cursor: text.trim() ? 'pointer' : 'default',
              transition: 'all 0.15s ease',
            }}
          >
            Submit
          </button>
        </>
      ) : (
        <div>
          {/* Model answer */}
          <div style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: COLORS.textTer, textTransform: 'uppercase', letterSpacing: '0.07em', marginBottom: 6 }}>
              Model Answer
            </div>
            <div
              style={{
                background: 'oklch(0.95 0.04 145)',
                border: `1px solid ${COLORS.easy}`,
                borderRadius: 10,
                padding: '11px 14px',
                fontSize: 14,
                color: COLORS.text,
                lineHeight: 1.55,
              }}
            >
              {modelAnswer}
            </div>
          </div>

          {/* User answer */}
          {text && (
            <div style={{ marginBottom: 14 }}>
              <div style={{ fontSize: 11, fontWeight: 600, color: COLORS.textTer, textTransform: 'uppercase', letterSpacing: '0.07em', marginBottom: 6 }}>
                Your Answer
              </div>
              <div
                style={{
                  background: COLORS.bg,
                  border: `1px solid ${COLORS.border}`,
                  borderRadius: 10,
                  padding: '11px 14px',
                  fontSize: 14,
                  color: COLORS.textSec,
                  lineHeight: 1.55,
                }}
              >
                {text}
              </div>
            </div>
          )}

          {/* Self-evaluation (UI_POLICY §7.3) */}
          {!selfEval ? (
            <div>
              <div style={{ fontSize: 13, fontWeight: 600, color: COLORS.text, marginBottom: 10 }}>
                Was your answer close?
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  onClick={() => handleSelfEval('yes')}
                  style={{
                    flex: 1, padding: '11px', borderRadius: 10, border: 'none',
                    background: 'oklch(0.95 0.04 145)', color: COLORS.easy,
                    fontSize: 14, fontWeight: 600, cursor: 'pointer',
                  }}
                >
                  Yes
                </button>
                <button
                  onClick={() => handleSelfEval('no')}
                  style={{
                    flex: 1, padding: '11px', borderRadius: 10, border: 'none',
                    background: 'oklch(0.95 0.04 28)', color: COLORS.hard,
                    fontSize: 14, fontWeight: 600, cursor: 'pointer',
                  }}
                >
                  No
                </button>
              </div>
            </div>
          ) : (
            <div
              style={{
                background: COLORS.accentLight,
                borderRadius: 10,
                padding: '12px 14px',
                fontSize: 13,
                color: COLORS.accent,
                fontWeight: 500,
                textAlign: 'center',
              }}
            >
              {selfEval === 'yes'
                ? 'Great work. Quiz complete.'
                : 'Keep at it — review the article and try again next time.'}
            </div>
          )}
        </div>
      )}
    </div>
  );
});
