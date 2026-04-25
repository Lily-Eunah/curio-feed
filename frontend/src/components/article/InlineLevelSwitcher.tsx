import { memo } from 'react';
import { COLORS, LEVEL_CONFIG } from '../../theme';
import type { DifficultyLevel } from '../../types';

interface Props {
  currentLevel: DifficultyLevel;
  onSwitch: (level: DifficultyLevel) => void;
}

const LEVELS: DifficultyLevel[] = ['EASY', 'MEDIUM', 'HARD'];

export default memo(function InlineLevelSwitcher({ currentLevel, onSwitch }: Props) {
  const idx = LEVELS.indexOf(currentLevel);
  const cfg = LEVEL_CONFIG[currentLevel];
  const canEasier = idx > 0;
  const canHarder = idx < 2;

  const segBtn = (
    label: React.ReactNode,
    isActive: boolean,
    disabled: boolean,
    onClick: (() => void) | null,
  ) => (
    <button
      onClick={disabled || !onClick ? undefined : onClick}
      style={{
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 4,
        padding: '9px 4px',
        background: isActive ? cfg.bg : 'transparent',
        border: 'none',
        cursor: disabled ? 'default' : 'pointer',
        fontSize: 12,
        fontWeight: isActive ? 700 : 500,
        color: isActive ? cfg.color : disabled ? COLORS.textTer : COLORS.textSec,
        transition: 'all 0.15s ease',
        opacity: disabled ? 0.35 : 1,
      }}
    >
      {label}
    </button>
  );

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'stretch',
        border: `1.5px solid ${COLORS.border}`,
        borderRadius: 10,
        overflow: 'hidden',
        marginBottom: 20,
      }}
    >
      {segBtn(<>← Easier</>, false, !canEasier, canEasier ? () => onSwitch(LEVELS[idx - 1]) : null)}
      <div style={{ width: 1, background: COLORS.border, flexShrink: 0 }} />
      {segBtn(cfg.label, true, false, null)}
      <div style={{ width: 1, background: COLORS.border, flexShrink: 0 }} />
      {segBtn(<>Harder →</>, false, !canHarder, canHarder ? () => onSwitch(LEVELS[idx + 1]) : null)}
    </div>
  );
});
