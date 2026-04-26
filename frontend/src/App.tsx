import { useState, useCallback } from 'react';
import { COLORS } from './theme';
import { ARTICLES } from './data/articles';
import type { AppState, DifficultyLevel, MCQResult, ShortAnswerResult, ContinueReadingState } from './types';
import Toast from './components/ui/Toast';

// ─── Lazy screen imports (avoid circular refs) ────────────────────────────────
import Onboarding from './components/Onboarding';
import Feed from './components/feed/Feed';
import Saved from './components/saved/Saved';
import ArticleDetail from './components/article/ArticleDetail';
import LevelSheet from './components/feed/LevelSheet';

// ─── LocalStorage ──────────────────────────────────────────────────────────────

function loadState(): Partial<AppState> | null {
  try {
    const raw = localStorage.getItem('curio_state');
    return raw ? (JSON.parse(raw) as Partial<AppState>) : null;
  } catch {
    return null;
  }
}

function saveState(state: AppState): void {
  try {
    localStorage.setItem('curio_state', JSON.stringify(state));
  } catch {
    // ignore storage errors
  }
}

const DEFAULT_STATE: AppState = {
  onboarded: false,
  userLevel: 'MEDIUM',
  selectedCategory: 'All',
  savedIds: [],
  readIds: [],
  visitedIds: [],
  continueReading: null,
  quizProgress: {},
};

type Screen = 'feed' | 'article' | 'saved';

// ─── App ──────────────────────────────────────────────────────────────────────

export default function App() {
  const [appState, setAppStateRaw] = useState<AppState>(() => {
    const saved = loadState();
    return saved ? { ...DEFAULT_STATE, ...saved } : DEFAULT_STATE;
  });
  const [screen, setScreen] = useState<Screen>('feed');
  const [currentArticleId, setCurrentArticleId] = useState<string | null>(null);
  const [levelSheetOpen, setLevelSheetOpen] = useState(false);
  const [toast, setToast] = useState({ message: '', visible: false });

  const setAppState = useCallback((updater: Partial<AppState> | ((prev: AppState) => AppState)) => {
    setAppStateRaw(prev => {
      const next = typeof updater === 'function' ? updater(prev) : { ...prev, ...updater };
      saveState(next);
      return next;
    });
  }, []);

  const showToast = useCallback((message: string) => {
    setToast({ message, visible: true });
    setTimeout(() => setToast(t => ({ ...t, visible: false })), 2200);
  }, []);

  const currentArticle = currentArticleId ? ARTICLES.find(a => a.id === currentArticleId) ?? null : null;

  const getNextArticle = useCallback((articleId: string) => {
    const idx = ARTICLES.findIndex(a => a.id === articleId);
    const rest = ARTICLES.slice(idx + 1).filter(a =>
      appState.selectedCategory === 'All' || a.category === ARTICLES[idx]?.category,
    );
    return rest[0] ?? null;
  }, [appState.selectedCategory]);

  // ── Handlers ─────────────────────────────────────────────────────────────────

  const handleOnboardingComplete = useCallback((level: DifficultyLevel) => {
    setAppState({ onboarded: true, userLevel: level });
    setTimeout(() => setScreen('feed'), 50);
  }, [setAppState]);

  const handleArticleTap = useCallback((id: string) => {
    setCurrentArticleId(id);
    setScreen('article');
    setAppState(prev => ({
      ...prev,
      visitedIds: prev.visitedIds.includes(id) ? prev.visitedIds : [...prev.visitedIds, id],
    }));
  }, [setAppState]);

  const handleBack = useCallback(() => {
    setCurrentArticleId(null);
    setScreen('feed');
  }, []);

  const handleNavigate = useCallback((tab: Screen | 'me') => {
    if (tab === 'me') { showToast('Profile coming soon'); return; }
    setScreen(tab);
  }, [showToast]);

  const handleLevelChange = useCallback((level: DifficultyLevel) => {
    setAppState({ userLevel: level });
    setLevelSheetOpen(false);
    showToast(`Switched to ${level[0] + level.slice(1).toLowerCase()}`);
  }, [setAppState, showToast]);

  const handleCategoryChange = useCallback((cat: string) => {
    setAppState({ selectedCategory: cat });
  }, [setAppState]);

  // Feed save: silent (no toast per UI_POLICY §6.1)
  const handleFeedSave = useCallback((articleId: string) => {
    setAppState(prev => {
      const isSaved = prev.savedIds.includes(articleId);
      return {
        ...prev,
        savedIds: isSaved ? prev.savedIds.filter(id => id !== articleId) : [...prev.savedIds, articleId],
      };
    });
  }, [setAppState]);

  // Article save: toast allowed per UI_POLICY §6.2
  const handleArticleSave = useCallback((articleId: string) => {
    setAppState(prev => {
      const isSaved = prev.savedIds.includes(articleId);
      const savedIds = isSaved
        ? prev.savedIds.filter(id => id !== articleId)
        : [...prev.savedIds, articleId];
      showToast(isSaved ? 'Removed from saved' : 'Saved for later');
      return { ...prev, savedIds };
    });
  }, [setAppState, showToast]);

  const handleRemoveSaved = useCallback((articleId: string) => {
    setAppState(prev => ({ ...prev, savedIds: prev.savedIds.filter(id => id !== articleId) }));
    showToast('Removed from saved');
  }, [setAppState, showToast]);

  const handleQuizAnswer = useCallback((
    articleId: string,
    qKey: 'q1' | 'q2' | 'q3',
    answer: MCQResult | ShortAnswerResult,
  ) => {
    setAppState(prev => {
      const prevProgress = prev.quizProgress[articleId] ?? {};
      const quizProgress = { ...prev.quizProgress, [articleId]: { ...prevProgress, [qKey]: answer } };
      const readIds = prev.readIds.includes(articleId) ? prev.readIds : [...prev.readIds, articleId];
      // Remove from continue reading once read (UI_POLICY §3.2)
      const continueReading = prev.continueReading?.articleId === articleId ? null : prev.continueReading;
      return { ...prev, quizProgress, readIds, continueReading };
    });
  }, [setAppState]);

  const handleScrollProgress = useCallback((articleId: string, scrollPosition: number, progress: number) => {
    setAppState(prev => {
      if (prev.readIds.includes(articleId)) return prev;
      // Replace any existing continue reading (only ONE slot, UI_POLICY §3.2)
      const continueReading: ContinueReadingState = { articleId, scrollPosition, progress, updatedAt: Date.now() };
      return { ...prev, continueReading };
    });
  }, [setAppState]);

  const handleLevelChangeFromArticle = useCallback((level: DifficultyLevel) => {
    setAppState({ userLevel: level });
  }, [setAppState]);

  const handleNextArticle = useCallback((id: string) => {
    setCurrentArticleId(id);
    setAppState(prev => ({
      ...prev,
      visitedIds: prev.visitedIds.includes(id) ? prev.visitedIds : [...prev.visitedIds, id],
    }));
  }, [setAppState]);

  // ── Render ────────────────────────────────────────────────────────────────────

  if (!appState.onboarded) {
    return (
      <div style={{ position: 'relative', width: '100%', height: '100%' }}>
        <Onboarding onComplete={handleOnboardingComplete} />
      </div>
    );
  }

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', background: COLORS.bg }}>

      {/* Feed */}
      <div style={{
        position: 'absolute', inset: 0,
        opacity: screen === 'feed' ? 1 : 0,
        pointerEvents: screen === 'feed' ? 'all' : 'none',
        transition: 'opacity 0.2s ease',
      }}>
        <Feed
          articles={ARTICLES}
          userLevel={appState.userLevel}
          selectedCategory={appState.selectedCategory}
          savedIds={appState.savedIds}
          readIds={appState.readIds}
          visitedIds={appState.visitedIds}
          continueReading={appState.continueReading}
          onArticleTap={handleArticleTap}
          onCategoryChange={handleCategoryChange}
          onLevelSheetOpen={() => setLevelSheetOpen(true)}
          onNavigate={handleNavigate}
          onSave={handleFeedSave}
        />
      </div>

      {/* Article (slide in from right) */}
      <div style={{
        position: 'absolute', inset: 0,
        transform: screen === 'article' ? 'translateX(0)' : 'translateX(100%)',
        transition: 'transform 0.3s cubic-bezier(0.32,0.72,0,1)',
      }}>
        {currentArticle && (
          <ArticleDetail
            article={currentArticle}
            userLevel={appState.userLevel}
            savedIds={appState.savedIds}
            quizProgress={appState.quizProgress}
            continueReading={appState.continueReading}
            onBack={handleBack}
            onSave={handleArticleSave}
            onQuizAnswer={handleQuizAnswer}
            onScrollProgress={handleScrollProgress}
            onLevelChange={handleLevelChangeFromArticle}
            nextArticle={getNextArticle(currentArticle.id)}
            onNextArticle={handleNextArticle}
          />
        )}
      </div>

      {/* Saved */}
      <div style={{
        position: 'absolute', inset: 0,
        opacity: screen === 'saved' ? 1 : 0,
        pointerEvents: screen === 'saved' ? 'all' : 'none',
        transition: 'opacity 0.2s ease',
      }}>
        <Saved
          articles={ARTICLES}
          savedIds={appState.savedIds}
          onArticleTap={handleArticleTap}
          onRemove={handleRemoveSaved}
          onNavigate={handleNavigate}
        />
      </div>

      {/* Level sheet (feed-level switcher) */}
      <div style={{ position: 'absolute', inset: 0, pointerEvents: levelSheetOpen ? 'all' : 'none', zIndex: 200 }}>
        <LevelSheet
          open={levelSheetOpen}
          currentLevel={appState.userLevel}
          onSelect={handleLevelChange}
          onClose={() => setLevelSheetOpen(false)}
        />
      </div>

      <Toast message={toast.message} visible={toast.visible} />
    </div>
  );
}
