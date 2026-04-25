import { memo } from 'react';
import { COLORS, CAT_COLORS } from '../../theme';
import type { Article } from '../../types';

interface Props {
  article: Article;
  onTap: () => void;
}

export default memo(function NextArticleCard({ article, onTap }: Props) {
  const catColor = CAT_COLORS[article.category]?.dot ?? COLORS.textSec;
  return (
    <div style={{ marginTop: 32 }}>
      <div
        style={{
          fontSize: 11,
          fontWeight: 700,
          color: COLORS.textTer,
          letterSpacing: '0.07em',
          textTransform: 'uppercase',
          marginBottom: 10,
        }}
      >
        Next Article
      </div>
      <button
        onClick={onTap}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          width: '100%',
          textAlign: 'left',
          background: COLORS.surface,
          border: `1.5px solid ${COLORS.border}`,
          borderRadius: 14,
          padding: '14px 16px',
          cursor: 'pointer',
          transition: 'border-color 0.15s ease',
        }}
        onMouseEnter={e => (e.currentTarget.style.borderColor = COLORS.accent)}
        onMouseLeave={e => (e.currentTarget.style.borderColor = COLORS.border)}
      >
        <div style={{ flex: 1, paddingRight: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 5 }}>
            <span
              style={{
                width: 7,
                height: 7,
                borderRadius: '50%',
                background: catColor,
                display: 'inline-block',
              }}
            />
            <span style={{ fontSize: 11, fontWeight: 600, color: catColor, letterSpacing: '0.04em' }}>
              {article.category.toUpperCase()}
            </span>
          </div>
          <div style={{ fontSize: 15, fontWeight: 600, color: COLORS.text, lineHeight: 1.3 }}>
            {article.title}
          </div>
        </div>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={COLORS.textSec} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>
    </div>
  );
});
