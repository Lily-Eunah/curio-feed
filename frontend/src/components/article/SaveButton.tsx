import { memo } from 'react';
import { COLORS } from '../../theme';

interface Props {
  isSaved: boolean;
  onToggle: () => void;
}

// Article save: text link per UI_POLICY §6.2
export default memo(function SaveButton({ isSaved, onToggle }: Props) {
  return (
    <button
      onClick={onToggle}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        width: '100%',
        padding: '13px',
        background: isSaved ? COLORS.accentLight : COLORS.surface,
        border: `1.5px solid ${isSaved ? COLORS.accent : COLORS.border}`,
        borderRadius: 12,
        cursor: 'pointer',
        fontSize: 14,
        fontWeight: 600,
        color: isSaved ? COLORS.accent : COLORS.textSec,
        transition: 'all 0.18s ease',
      }}
    >
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill={isSaved ? COLORS.accent : 'none'}
        stroke={isSaved ? COLORS.accent : 'currentColor'}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
      </svg>
      {isSaved ? 'Saved' : 'Save for later'}
    </button>
  );
});
