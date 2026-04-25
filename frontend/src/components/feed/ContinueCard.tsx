import { memo } from 'react';
import { COLORS, CAT_COLORS } from '../../theme';
import type { Article } from '../../types';

interface Props {
  article: Article;
  updatedAt: number;
  onTap: () => void;
}

function timeAgo(ts: number): string {
  const mins = Math.floor((Date.now() - ts) / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins} min ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs} hr ago`;
  const days = Math.floor(hrs / 24);
  return `${days} day${days > 1 ? 's' : ''} ago`;
}

export default memo(function ContinueCard({ article, updatedAt, onTap }: Props) {
  const catColor = CAT_COLORS[article.category]?.dot ?? COLORS.textSec;
  return (
    <button
      onClick={onTap}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        width: '100%',
        textAlign: 'left',
        background: COLORS.accentLight,
        border: `1.5px solid ${COLORS.accentMid}`,
        borderRadius: 14,
        padding: '14px 16px',
        cursor: 'pointer',
        gap: 12,
      }}
    >
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 5 }}>
          <span
            style={{
              width: 7,
              height: 7,
              borderRadius: '50%',
              background: catColor,
              display: 'inline-block',
              flexShrink: 0,
            }}
          />
          <span style={{ fontSize: 11, fontWeight: 600, color: catColor, letterSpacing: '0.04em' }}>
            {article.category.toUpperCase()}
          </span>
        </div>
        <div
          style={{
            fontSize: 15,
            fontWeight: 600,
            color: COLORS.text,
            lineHeight: 1.3,
            marginBottom: 4,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {article.title}
        </div>
        {/* UI_POLICY §3.3: show "Last read X min ago", no percentage, no progress bar */}
        <div style={{ fontSize: 12, color: COLORS.accent, fontWeight: 500 }}>
          Last read {timeAgo(updatedAt)}
        </div>
      </div>
      <svg
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill="none"
        stroke={COLORS.accent}
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        style={{ flexShrink: 0 }}
      >
        <polyline points="9 18 15 12 9 6" />
      </svg>
    </button>
  );
});
