import { useState } from 'react';
import { COLORS, LEVEL_CONFIG } from '../theme';
import type { DifficultyLevel } from '../types';

interface Props {
  onComplete: (level: DifficultyLevel) => void;
}

const LEVEL_SAMPLES: Record<DifficultyLevel, string> = {
  EASY: '"Many people are moving to smaller homes to save money and live more simply."',
  MEDIUM: '"A growing number of young professionals are choosing smaller living spaces as urban housing costs continue to rise."',
  HARD: '"An emerging demographic shift toward compact urban dwelling reflects broader anxieties about affordability, sustainability, and the perceived superfluity of space."',
};

const LEVELS: DifficultyLevel[] = ['EASY', 'MEDIUM', 'HARD'];

export default function Onboarding({ onComplete }: Props) {
  const [selected, setSelected] = useState<DifficultyLevel | null>(null);

  const handleSelect = (level: DifficultyLevel) => {
    setSelected(level);
    setTimeout(() => onComplete(level), 600);
  };

  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        background: COLORS.bg,
        display: 'flex',
        flexDirection: 'column',
        padding: '48px 24px 40px',
        overflowY: 'auto',
      }}
    >
      <div style={{ marginBottom: 48 }}>
        <div style={{ fontSize: 26, fontWeight: 700, color: COLORS.text, letterSpacing: '-0.03em' }}>
          curio<span style={{ color: COLORS.accent }}>.</span>
        </div>
      </div>

      <div style={{ flex: 1 }}>
        <h1 style={{ fontSize: 28, fontWeight: 700, color: COLORS.text, letterSpacing: '-0.03em', lineHeight: 1.2, margin: '0 0 10px' }}>
          Choose your reading level
        </h1>
        <p style={{ fontSize: 15, color: COLORS.textSec, margin: '0 0 32px', lineHeight: 1.5 }}>
          You can always change this later.
        </p>

        {LEVELS.map(level => {
          const cfg = LEVEL_CONFIG[level];
          const isSelected = selected === level;
          return (
            <button
              key={level}
              onClick={() => handleSelect(level)}
              style={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                background: isSelected ? cfg.bg : COLORS.surface,
                border: `2px solid ${isSelected ? cfg.color : COLORS.border}`,
                borderRadius: 16,
                padding: '16px 18px',
                marginBottom: 10,
                cursor: 'pointer',
                transition: 'all 0.18s ease',
                transform: isSelected ? 'scale(0.98)' : 'scale(1)',
              }}
            >
              <p
                style={{
                  fontSize: 15,
                  color: COLORS.text,
                  fontFamily: 'Lora, Georgia, serif',
                  lineHeight: 1.6,
                  margin: '0 0 10px',
                }}
              >
                {LEVEL_SAMPLES[level]}
              </p>
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 5,
                  fontSize: 11,
                  fontWeight: 700,
                  color: cfg.color,
                  letterSpacing: '0.06em',
                  textTransform: 'uppercase',
                }}
              >
                <span style={{ width: 6, height: 6, borderRadius: '50%', background: cfg.color, display: 'inline-block' }} />
                {cfg.label}
              </span>
            </button>
          );
        })}
      </div>

      <p style={{ fontSize: 12, color: COLORS.textTer, textAlign: 'center', marginTop: 16 }}>
        Curio helps you read at your level — and grow.
      </p>
    </div>
  );
}
