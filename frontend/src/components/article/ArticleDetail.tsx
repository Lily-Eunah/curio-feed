import { useState, useEffect, useRef, useCallback } from 'react';
import { COLORS, CAT_COLORS, LEVEL_CONFIG } from '../../theme';
import type {
  Article, DifficultyLevel, ArticleQuizProgress,
  ContinueReadingState, MCQResult, ShortAnswerResult, VocabEntry,
} from '../../types';
import Divider from '../ui/Divider';
import ArticleBody from './ArticleBody';
import InlineLevelSwitcher from './InlineLevelSwitcher';
import VocabSheet from './VocabSheet';
import VocabReview from './VocabReview';
import SaveButton from './SaveButton';
import QuizSection from './quiz/QuizSection';
import NextArticleCard from './NextArticleCard';

interface Props {
  article: Article;
  userLevel: DifficultyLevel;
  savedIds: string[];
  quizProgress: Record<string, ArticleQuizProgress>;
  continueReading: ContinueReadingState | null;
  onBack: () => void;
  onSave: (id: string) => void;
  onQuizAnswer: (articleId: string, qKey: 'q1' | 'q2' | 'q3', answer: MCQResult | ShortAnswerResult) => void;
  onScrollProgress: (articleId: string, scrollPosition: number, progress: number) => void;
  onLevelChange: (level: DifficultyLevel) => void;
  nextArticle: Article | null;
  onNextArticle: (id: string) => void;
}

export default function ArticleDetail({
  article, userLevel, savedIds, quizProgress, continueReading,
  onBack, onSave, onQuizAnswer, onScrollProgress, onLevelChange, nextArticle, onNextArticle,
}: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const bodyRef = useRef<HTMLDivElement>(null);
  const [vocabSheet, setVocabSheet] = useState<VocabEntry | null>(null);
  const [currentLevel, setCurrentLevel] = useState<DifficultyLevel>(userLevel);
  const [levelToast, setLevelToast] = useState<string | null>(null);

  const articleId = article.id;
  const isSaved = savedIds.includes(articleId);
  const progress = quizProgress[articleId] ?? {};
  const answeredCount = Object.keys(progress).length;

  // Restore scroll from continue reading (UI_POLICY §3)
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    if (continueReading?.articleId === articleId && continueReading.scrollPosition > 0) {
      setTimeout(() => { el.scrollTop = continueReading.scrollPosition; }, 60);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Scroll tracking: 1s throttle, ≥25% body progress (UI_POLICY §3.1, Performance)
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    let throttleTimer: ReturnType<typeof setTimeout> | null = null;

    const handleScroll = () => {
      if (throttleTimer) return;
      throttleTimer = setTimeout(() => {
        throttleTimer = null;
        const body = bodyRef.current;
        if (!body) return;
        const scrollTop = el.scrollTop;
        const bodyTop = body.offsetTop;
        const bodyHeight = body.offsetHeight;
        const viewportH = el.clientHeight;
        const seen = scrollTop + viewportH - bodyTop;
        const bodyPct = Math.max(0, Math.min(100, (seen / bodyHeight) * 100));
        if (bodyPct >= 25) {
          onScrollProgress(articleId, scrollTop, Math.round(bodyPct));
        }
      }, 1000);
    };

    el.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      el.removeEventListener('scroll', handleScroll);
      if (throttleTimer) clearTimeout(throttleTimer);
    };
  }, [articleId, onScrollProgress]);

  const handleLevelSwitch = useCallback((lvl: DifficultyLevel) => {
    setCurrentLevel(lvl);
    const labels = LEVEL_CONFIG[lvl]?.label ?? lvl;
    setLevelToast(`Switched to ${labels}. Restarting from the top.`);
    if (scrollRef.current) scrollRef.current.scrollTop = 0;
    setTimeout(() => setLevelToast(null), 2500);
    onLevelChange(lvl);
  }, [onLevelChange]);

  const catColor = CAT_COLORS[article.category]?.dot ?? COLORS.textSec;

  return (
    <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', background: COLORS.bg }}>
      {/* Header: back only (UI_POLICY §4.1) */}
      <div
        style={{
          padding: '52px 16px 12px',
          background: COLORS.bg,
          borderBottom: `1px solid ${COLORS.borderLight}`,
          flexShrink: 0,
        }}
      >
        <button
          onClick={onBack}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 4,
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: COLORS.textSec,
            fontSize: 14,
            fontWeight: 500,
            padding: '4px 0',
          }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back
        </button>
      </div>

      {/* Scrollable body */}
      <div ref={scrollRef} style={{ flex: 1, overflowY: 'auto', scrollbarWidth: 'none' }}>
        <div style={{ padding: '20px 20px 40px' }}>

          {/* Level switcher (not sticky, UI_POLICY §4.2) */}
          <InlineLevelSwitcher currentLevel={currentLevel} onSwitch={handleLevelSwitch} />

          {/* Meta line */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 12 }}>
            <span style={{ width: 7, height: 7, borderRadius: '50%', background: catColor, display: 'inline-block' }} />
            <span style={{ fontSize: 11, fontWeight: 600, color: catColor, letterSpacing: '0.06em' }}>
              {article.category.toUpperCase()}
            </span>
            <span style={{ width: 2, height: 2, borderRadius: '50%', background: COLORS.textTer, display: 'inline-block' }} />
            <span style={{ fontSize: 11, color: COLORS.textTer }}>{article.date}</span>
            <span style={{ width: 2, height: 2, borderRadius: '50%', background: COLORS.textTer, display: 'inline-block' }} />
            <span style={{ fontSize: 11, color: COLORS.textTer }}>{article.readTime}</span>
          </div>

          {/* Title */}
          <h1
            style={{
              fontSize: 24,
              fontWeight: 700,
              color: COLORS.text,
              lineHeight: 1.25,
              letterSpacing: '-0.03em',
              margin: '0 0 20px',
            }}
          >
            {article.title}
          </h1>

          {/* Article body (scroll-tracked) */}
          <ArticleBody
            body={article.body}
            vocabulary={article.vocabulary}
            onVocabTap={setVocabSheet}
            bodyRef={bodyRef}
          />

          {/* Vocab tap hint */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              background: COLORS.accentLight,
              borderRadius: 10,
              padding: '10px 14px',
              marginBottom: 32,
              marginTop: 4,
            }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={COLORS.accent} strokeWidth="2" strokeLinecap="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span style={{ fontSize: 12, color: COLORS.accent, fontWeight: 500 }}>
              Tap underlined words to see definitions
            </span>
          </div>

          <Divider />
          <VocabReview vocabulary={article.vocabulary} />
          <Divider />
          <SaveButton isSaved={isSaved} onToggle={() => onSave(articleId)} />
          <Divider />

          <QuizSection
            articleId={articleId}
            quiz={article.quiz}
            progress={progress}
            onAnswer={onQuizAnswer}
          />

          {/* Next article: shown after ≥1 quiz answered (UI_POLICY §10) */}
          {nextArticle && answeredCount >= 1 && (
            <NextArticleCard article={nextArticle} onTap={() => onNextArticle(nextArticle.id)} />
          )}
        </div>
      </div>

      {/* Vocab bottom sheet (UI_POLICY §5.2) */}
      <VocabSheet vocab={vocabSheet} onClose={() => setVocabSheet(null)} />

      {/* Level switch toast (UI_POLICY §4.2: 2.5s, bottom of screen) */}
      {levelToast && (
        <div
          style={{
            position: 'absolute',
            bottom: 24,
            left: 16,
            right: 16,
            background: COLORS.text,
            color: '#fff',
            padding: '11px 16px',
            borderRadius: 12,
            fontSize: 13,
            fontWeight: 500,
            boxShadow: '0 4px 16px rgba(0,0,0,0.2)',
            zIndex: 300,
          }}
        >
          {levelToast}
        </div>
      )}
    </div>
  );
}
