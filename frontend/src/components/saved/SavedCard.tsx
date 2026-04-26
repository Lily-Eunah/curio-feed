import { memo } from 'react';
import { COLORS, CAT_COLORS } from '../../theme';
import type { Article } from '../../types';

interface Props {
  article: Article;
  onTap: () => void;
  onRemove: () => void;
}

export default memo(function SavedCard({ article, onTap, onRemove }: Props) {
  const catColor = CAT_COLORS[article.category]?.dot ?? COLORS.textSec;
  return (
    <div
      style={{
        background: COLORS.surface,
        border: `1px solid ${COLORS.borderLight}`,
        borderRadius: 14,
        padding: '14px 16px',
        marginBottom: 8,
        display: 'flex',
        gap: 12,
      }}
    >
      {/* Tap area (UI_POLICY §8.2) */}
      <button
        onClick={onTap}
        style={{ flex: 1, textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 6 }}>
          <span style={{ width: 7, height: 7, borderRadius: '50%', background: catColor, display: 'inline-block' }} />
          <span style={{ fontSize: 11, fontWeight: 600, color: catColor, letterSpacing: '0.04em' }}>
            {article.category.toUpperCase()}
          </span>
        </div>
        <div style={{ fontSize: 15, fontWeight: 600, color: COLORS.text, lineHeight: 1.3, marginBottom: 4 }}>
          {article.title}
        </div>
        {/* UI_POLICY §8.1: show title + reading time + category only */}
        <div style={{ fontSize: 11, color: COLORS.textTer }}>
          {article.readTime}
        </div>
      </button>

      {/* Remove button (UI_POLICY §8.2) */}
      <button
        onClick={onRemove}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          padding: '0 0 0 4px',
          alignSelf: 'flex-start',
          color: COLORS.textTer,
          flexShrink: 0,
        }}
        aria-label="Remove from saved"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  );
});
