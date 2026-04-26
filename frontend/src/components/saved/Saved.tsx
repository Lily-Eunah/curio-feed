import { useMemo } from 'react';
import { COLORS } from '../../theme';
import type { Article } from '../../types';
import NavBar from '../ui/NavBar';
import SavedCard from './SavedCard';

type Screen = 'feed' | 'saved' | 'me';

interface Props {
  articles: Article[];
  savedIds: string[];
  onArticleTap: (id: string) => void;
  onRemove: (id: string) => void;
  onNavigate: (tab: Screen | 'me') => void;
}

export default function Saved({ articles, savedIds, onArticleTap, onRemove, onNavigate }: Props) {
  const savedArticles = useMemo(
    () => articles.filter(a => savedIds.includes(a.id)),
    [articles, savedIds],
  );

  return (
    <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column' }}>
      <div
        style={{
          padding: '52px 20px 16px',
          background: COLORS.bg,
          borderBottom: `1px solid ${COLORS.borderLight}`,
          flexShrink: 0,
        }}
      >
        <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, letterSpacing: '-0.03em' }}>
          Saved
        </div>
        <div style={{ fontSize: 13, color: COLORS.textSec, marginTop: 2 }}>
          {savedArticles.length} article{savedArticles.length !== 1 ? 's' : ''}
        </div>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 16px 72px', scrollbarWidth: 'none' }}>
        {savedArticles.length === 0 ? (
          /* Empty state (UI_POLICY §9) */
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              textAlign: 'center',
              padding: 24,
            }}
          >
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke={COLORS.textTer} strokeWidth="1.5" style={{ marginBottom: 16 }}>
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
            </svg>
            <div style={{ fontSize: 17, fontWeight: 600, color: COLORS.text, marginBottom: 8 }}>
              No saved articles yet
            </div>
            <div style={{ fontSize: 14, color: COLORS.textSec, lineHeight: 1.5, marginBottom: 24 }}>
              Save articles from the feed to read them later.
            </div>
            <button
              onClick={() => onNavigate('feed')}
              style={{
                background: COLORS.text,
                color: '#fff',
                border: 'none',
                borderRadius: 20,
                padding: '10px 20px',
                fontSize: 14,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Go to Feed
            </button>
          </div>
        ) : (
          savedArticles.map(a => (
            <SavedCard
              key={a.id}
              article={a}
              onTap={() => onArticleTap(a.id)}
              onRemove={() => onRemove(a.id)}
            />
          ))
        )}
      </div>

      <NavBar active="saved" onNavigate={onNavigate} />
    </div>
  );
}
