import { useState, useEffect, useMemo } from 'react';
import { COLORS, CATEGORIES } from '../../theme';
import type { Article, DifficultyLevel, ContinueReadingState } from '../../types';
import NavBar from '../ui/NavBar';
import CategoryPill from '../ui/CategoryPill';
import LevelBadge from '../ui/LevelBadge';
import { SkeletonCard } from '../ui/Shimmer';
import SectionLabel from './SectionLabel';
import ArticleCard from './ArticleCard';
import ContinueCard from './ContinueCard';
import EmptyFeed from './EmptyFeed';

type Screen = 'feed' | 'saved' | 'me';

interface Props {
  articles: Article[];
  userLevel: DifficultyLevel;
  selectedCategory: string;
  savedIds: string[];
  readIds: string[];
  visitedIds: string[];
  continueReading: ContinueReadingState | null;
  onArticleTap: (id: string) => void;
  onCategoryChange: (cat: string) => void;
  onLevelSheetOpen: () => void;
  onNavigate: (tab: Screen | 'me') => void;
  onSave: (id: string) => void;
}

function getGroupLabel(dateStr: string): string {
  const fmt = (d: Date) => d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (dateStr === fmt(today)) return 'Today';
  if (dateStr === fmt(yesterday)) return 'Yesterday';
  return dateStr;
}

export default function Feed({
  articles, userLevel, selectedCategory, savedIds, readIds, visitedIds,
  continueReading, onArticleTap, onCategoryChange, onLevelSheetOpen, onNavigate, onSave,
}: Props) {
  const [loading, setLoading] = useState(true);

  // Skeleton on initial mount and category change
  useEffect(() => {
    setLoading(true);
    const t = setTimeout(() => setLoading(false), 600);
    return () => clearTimeout(t);
  }, [selectedCategory]);

  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 600);
    return () => clearTimeout(t);
  }, []);

  const filtered = useMemo(
    () => articles.filter(a => selectedCategory === 'All' || a.category === selectedCategory),
    [articles, selectedCategory],
  );

  // Date-grouped articles
  const { groupMap, groupOrder } = useMemo(() => {
    const map: Record<string, Article[]> = {};
    const order: string[] = [];
    filtered.forEach(a => {
      const label = getGroupLabel(a.date);
      if (!map[label]) { map[label] = []; order.push(label); }
      map[label].push(a);
    });
    return { groupMap: map, groupOrder: order };
  }, [filtered]);

  // Continue reading slot: article that has ≥25% scroll progress and is not read (UI_POLICY §3.1)
  const continueArticle = useMemo(() => {
    if (!continueReading) return null;
    return articles.find(a => a.id === continueReading.articleId && !readIds.includes(a.id)) ?? null;
  }, [continueReading, articles, readIds]);

  return (
    <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <div
        style={{
          padding: '52px 20px 0',
          background: COLORS.bg,
          borderBottom: `1px solid ${COLORS.borderLight}`,
          flexShrink: 0,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, letterSpacing: '-0.03em' }}>
            curio<span style={{ color: COLORS.accent }}>.</span>
          </div>
          <button
            onClick={onLevelSheetOpen}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 4,
              background: COLORS.surface,
              border: `1.5px solid ${COLORS.border}`,
              borderRadius: 20,
              padding: '5px 10px 5px 8px',
              cursor: 'pointer',
            }}
            aria-label="Change reading level"
          >
            <LevelBadge level={userLevel} />
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke={COLORS.textSec} strokeWidth="2.5" strokeLinecap="round">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </button>
        </div>

        {/* Category filter */}
        <div
          style={{
            display: 'flex',
            gap: 6,
            overflowX: 'auto',
            paddingBottom: 14,
            scrollbarWidth: 'none',
          }}
        >
          {CATEGORIES.map(c => (
            <CategoryPill
              key={c}
              category={c}
              active={selectedCategory === c}
              onClick={() => onCategoryChange(c)}
            />
          ))}
        </div>
      </div>

      {/* Scrollable content */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 16px 72px', scrollbarWidth: 'none' }}>
        {loading ? (
          <>
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </>
        ) : filtered.length === 0 ? (
          <EmptyFeed selectedCategory={selectedCategory} onCategoryChange={onCategoryChange} />
        ) : (
          <>
            {/* Continue Reading slot (UI_POLICY §3) */}
            {continueArticle && continueReading && (
              <div style={{ marginBottom: 20 }}>
                <SectionLabel>Continue Reading</SectionLabel>
                <ContinueCard
                  article={continueArticle}
                  updatedAt={continueReading.updatedAt}
                  onTap={() => onArticleTap(continueArticle.id)}
                />
              </div>
            )}

            {/* Date-grouped article cards */}
            {groupOrder.map(label => (
              <div key={label} style={{ marginBottom: 20 }}>
                <SectionLabel>{label}</SectionLabel>
                {groupMap[label].map(a => (
                  <ArticleCard
                    key={a.id}
                    article={a}
                    isVisited={visitedIds.includes(a.id)}
                    isSaved={savedIds.includes(a.id)}
                    onTap={() => onArticleTap(a.id)}
                    onSave={onSave}
                  />
                ))}
              </div>
            ))}
          </>
        )}
      </div>

      <NavBar active="feed" onNavigate={onNavigate} />
    </div>
  );
}
