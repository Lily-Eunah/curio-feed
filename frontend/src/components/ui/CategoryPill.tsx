import { memo } from 'react';
import { CAT_COLORS, COLORS } from '../../theme';

interface Props {
  category: string;
  active: boolean;
  onClick: () => void;
}

export default memo(function CategoryPill({ category, active, onClick }: Props) {
  const c = CAT_COLORS[category] ?? CAT_COLORS['All'];
  return (
    <button
      onClick={onClick}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        padding: '6px 12px',
        background: active ? c.bg : 'transparent',
        border: `1.5px solid ${active ? c.dot : COLORS.border}`,
        borderRadius: 20,
        cursor: 'pointer',
        fontSize: 13,
        fontWeight: active ? 600 : 400,
        color: active ? c.dot : COLORS.textSec,
        transition: 'all 0.15s ease',
        whiteSpace: 'nowrap',
        flexShrink: 0,
      }}
    >
      {category !== 'All' && (
        <span
          style={{
            width: 6,
            height: 6,
            borderRadius: '50%',
            background: active ? c.dot : COLORS.textTer,
            display: 'inline-block',
          }}
        />
      )}
      {category}
    </button>
  );
});
