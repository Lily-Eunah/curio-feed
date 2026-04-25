import { useState, memo } from 'react';
import { COLORS, CAT_COLORS } from '../../theme';
import type { Article } from '../../types';

interface Props {
  article: Article;
  isVisited: boolean;
  isSaved: boolean;
  onTap: () => void;
  onSave: (id: string) => void;
}

function CategoryDot({ category }: { category: string }) {
  return (
    <span
      style={{
        width: 7,
        height: 7,
        borderRadius: '50%',
        background: CAT_COLORS[category]?.dot ?? COLORS.textTer,
        display: 'inline-block',
        flexShrink: 0,
      }}
    />
  );
}

export default memo(function ArticleCard({ article, isVisited, isSaved, onTap, onSave }: Props) {
  const [pressed, setPressed] = useState(false);
  const catColor = CAT_COLORS[article.category]?.dot ?? COLORS.textSec;

  return (
    <div
      style={{
        background: COLORS.surface,
        border: `1px solid ${COLORS.borderLight}`,
        borderRadius: 14,
        padding: '14px 16px',
        marginBottom: 8,
        // Visited: opacity 0.5 (UI_POLICY §2.4)
        opacity: isVisited ? 0.5 : 1,
        transform: pressed ? 'scale(0.985)' : 'scale(1)',
        transition: 'transform 0.1s ease, opacity 0.2s ease',
      }}
    >
      {/* Top row: category + save icon */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <CategoryDot category={article.category} />
          <span style={{ fontSize: 11, fontWeight: 600, color: catColor, letterSpacing: '0.04em' }}>
            {article.category.toUpperCase()}
          </span>
        </div>
        {/* Save button — stops propagation, no toast per UI_POLICY §2.3 */}
        <button
          onClick={e => { e.stopPropagation(); onSave(article.id); }}
          style={{
            width: 32,
            height: 32,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: isSaved ? COLORS.accent : COLORS.textTer,
            transition: 'color 0.15s ease',
            margin: '-6px -6px -6px 0',
          }}
          aria-label={isSaved ? 'Remove from saved' : 'Save article'}
        >
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill={isSaved ? COLORS.accent : 'none'}
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
          </svg>
        </button>
      </div>

      {/* Card body — full tap area */}
      <button
        onMouseDown={() => setPressed(true)}
        onMouseUp={() => setPressed(false)}
        onTouchStart={() => setPressed(true)}
        onTouchEnd={() => setPressed(false)}
        onClick={onTap}
        style={{ display: 'block', width: '100%', textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
      >
        <h3
          style={{
            fontSize: 16,
            fontWeight: 600,
            color: COLORS.text,
            lineHeight: 1.35,
            margin: '0 0 6px',
            letterSpacing: '-0.01em',
          }}
        >
          {article.title}
        </h3>
        <p style={{ fontSize: 13, color: COLORS.textSec, lineHeight: 1.5, margin: '0 0 10px' }}>
          {article.excerpt}
        </p>
        <span style={{ fontSize: 11, color: COLORS.textTer }}>
          {article.readTime}
        </span>
      </button>
    </div>
  );
});
