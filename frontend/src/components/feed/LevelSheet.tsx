import { memo } from 'react';
import BottomSheet from '../ui/BottomSheet';
import { COLORS, LEVEL_CONFIG } from '../../theme';
import type { DifficultyLevel } from '../../types';

interface Props {
  open: boolean;
  currentLevel: DifficultyLevel;
  onSelect: (level: DifficultyLevel) => void;
  onClose: () => void;
}

const LEVELS: DifficultyLevel[] = ['EASY', 'MEDIUM', 'HARD'];

export default memo(function LevelSheet({ open, currentLevel, onSelect, onClose }: Props) {
  return (
    <BottomSheet open={open} onClose={onClose} title="Reading Level">
      <div style={{ padding: '8px 16px 24px' }}>
        {LEVELS.map(level => {
          const cfg = LEVEL_CONFIG[level];
          const isActive = currentLevel === level;
          return (
            <button
              key={level}
              onClick={() => onSelect(level)}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                width: '100%',
                textAlign: 'left',
                background: isActive ? cfg.bg : 'transparent',
                border: `1.5px solid ${isActive ? cfg.color : COLORS.border}`,
                borderRadius: 12,
                padding: '14px 16px',
                marginBottom: 8,
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              <div>
                <div style={{ fontSize: 15, fontWeight: 600, color: isActive ? cfg.color : COLORS.text, marginBottom: 2 }}>
                  {cfg.label}
                </div>
                <div style={{ fontSize: 12, color: COLORS.textSec }}>{cfg.desc}</div>
              </div>
              {isActive && (
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={cfg.color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              )}
            </button>
          );
        })}
      </div>
    </BottomSheet>
  );
});
