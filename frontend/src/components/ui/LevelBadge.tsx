import { memo } from 'react';
import { LEVEL_CONFIG } from '../../theme';
import type { DifficultyLevel } from '../../types';

interface Props {
  level: DifficultyLevel;
  size?: 'sm' | 'md';
}

export default memo(function LevelBadge({ level, size = 'sm' }: Props) {
  const cfg = LEVEL_CONFIG[level] ?? { color: '#888', bg: '#f0f0f0', label: level };
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        background: cfg.bg,
        color: cfg.color,
        padding: size === 'sm' ? '2px 8px' : '4px 10px',
        borderRadius: 20,
        fontSize: size === 'sm' ? 11 : 12,
        fontWeight: 600,
        letterSpacing: '0.02em',
      }}
    >
      <span
        style={{
          width: 5,
          height: 5,
          borderRadius: '50%',
          background: cfg.color,
          display: 'inline-block',
        }}
      />
      {cfg.label}
    </span>
  );
});
