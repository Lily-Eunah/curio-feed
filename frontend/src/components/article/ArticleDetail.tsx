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
import { formatDate } from '../../utils/article';

interface Props {
  article: Article;
  userLevel: DifficultyLevel;
  savedIds: string[];
  quizProgress: Record<string, ArticleQuizProgress>;
  continueReading: ContinueReadingState | null;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  onBack: () => void;
  onSave: (id: string) => void;
  onQuizAnswer: (articleId: string, qKey: 'q1' | 'q2' | 'q3', answer: MCQResult | ShortAnswerResult) => void;
  onScrollProgress: (articleId: string, scrollPosition: number, progress: number) => void;
  onLevelChange: (level: DifficultyLevel) => void;
  nextArticle: Article | null;
  onNextArticle: (id: string) => void;
}

export default function ArticleDetail({
  article, userLevel, savedIds, quizProgress, continueReading, isLoading, error, onRetry,
  onBack, onSave, onQuizAnswer, onScrollProgress, onLevelChange, nextArticle, onNextArticle,
}: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const bodyRef = useRef<HTMLDivElement>(null);
  const audioRef = useRef<HTMLAudioElement>(null);
  const [vocabSheet, setVocabSheet] = useState<VocabEntry | null>(null);
  const [currentLevel, setCurrentLevel] = useState<DifficultyLevel>(userLevel);
  const [levelToast, setLevelToast] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoadingAudio, setIsLoadingAudio] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);

  // Sync with prop (Problem 3)
  useEffect(() => {
    setCurrentLevel(userLevel);
  }, [userLevel]);

  const articleId = article.id;
  const isSaved = savedIds.includes(articleId);
  const levelKey = `${articleId}:${currentLevel}`;
  const progress = quizProgress[levelKey] ?? {};
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

  // Pause audio when level changes or component unmounts
  useEffect(() => {
    const audioEl = audioRef.current;
    return () => {
      if (audioEl) {
        audioEl.pause();
      }
    };
  }, [articleId]);

  useEffect(() => {
    if (audioRef.current && isPlaying) {
      audioRef.current.pause();
    }
    setIsPlaying(false);
    setCurrentTime(0);
    setDuration(0);
    setIsLoadingAudio(false);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentLevel, articleId]);

  const handlePlay = useCallback(() => {
    if (audioRef.current) {
      if (audioRef.current.readyState < 3) {
        setIsLoadingAudio(true);
      }
      audioRef.current.play().catch(e => {
        console.error("Audio play failed:", e);
        setIsLoadingAudio(false);
      });
    }
  }, []);

  const handlePause = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
    }
  }, []);

  const handleSeek = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const newTime = Number(e.target.value);
    setCurrentTime(newTime);
    if (audioRef.current) {
      audioRef.current.currentTime = newTime;
    }
  }, []);

  const formatTime = (time: number) => {
    if (isNaN(time) || !isFinite(time)) return "00:00";
    const m = Math.floor(time / 60).toString().padStart(2, '0');
    const s = Math.floor(time % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

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
          {article.body === '' ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 300, padding: '40px 20px' }}>
              {error ? (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, textAlign: 'center' }}>
                  <p style={{ color: COLORS.textSec, fontSize: 14, lineHeight: 1.5, margin: 0 }}>
                    {error}
                  </p>
                  <button
                    onClick={onRetry}
                    style={{
                      padding: '10px 24px',
                      borderRadius: 20,
                      background: COLORS.accent,
                      color: '#fff',
                      border: 'none',
                      fontWeight: 600,
                      fontSize: 14,
                      cursor: 'pointer',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                    }}
                  >
                    Retry
                  </button>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
                  <div className="spinner" style={{ width: 34, height: 34, border: `3.5px solid ${COLORS.accentLight}`, borderTopColor: COLORS.accent, borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
                  <span style={{ fontSize: 14, color: COLORS.textSec, fontWeight: 500 }}>
                    Loading article details...
                  </span>
                </div>
              )}
            </div>
          ) : (
            <>
              {/* Level switcher (not sticky, UI_POLICY §4.2) */}
              <InlineLevelSwitcher
                currentLevel={currentLevel}
                onSwitch={handleLevelSwitch}
                disabled={isLoading}
                availableLevels={article.availableLevels}
              />

              {/* Loading Overlay (Problem 3) - now covers title/meta/body/quiz */}
              <div style={{ position: 'relative', minHeight: 400 }}>
                {isLoading && (
                  <div
                    style={{
                      position: 'absolute',
                      inset: -8, // slight offset to cover padding
                      background: 'rgba(250,250,248,0.8)',
                      backdropFilter: 'blur(5px)',
                      zIndex: 10,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      borderRadius: 16,
                    }}
                  >
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
                      <div className="spinner" style={{ width: 34, height: 34, border: `3.5px solid ${COLORS.accentLight}`, borderTopColor: COLORS.accent, borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
                      <span style={{ fontSize: 13, color: COLORS.accent, fontWeight: 600, letterSpacing: '0.02em' }}>
                        Switching to {LEVEL_CONFIG[currentLevel]?.label ?? currentLevel}
                      </span>
                    </div>
                  </div>
                )}

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

                {/* Native Audio Element & Controls */}
                <style>{`@keyframes spin { 100% { transform: rotate(360deg); } }`}</style>
                {article.audioUrl ? (
                  <>
                    <audio
                      ref={audioRef}
                      src={article.audioUrl}
                      preload="auto"
                      onWaiting={() => setIsLoadingAudio(true)}
                      onPlaying={() => setIsLoadingAudio(false)}
                      onCanPlay={() => setIsLoadingAudio(false)}
                      onPlay={() => setIsPlaying(true)}
                      onPause={() => setIsPlaying(false)}
                      onEnded={() => { setIsPlaying(false); setCurrentTime(0); }}
                      onTimeUpdate={(e) => setCurrentTime(e.currentTarget.currentTime)}
                      onLoadedMetadata={(e) => setDuration(e.currentTarget.duration)}
                      onError={() => { console.error("Failed to load audio"); setIsLoadingAudio(false); }}
                    />

                    {/* TTS Controls */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20, background: COLORS.bg, padding: '12px 16px', borderRadius: 12, border: `1px solid ${COLORS.borderLight}` }}>
                      {/* Play / Pause button */}
                      <button
                        onClick={!isPlaying ? handlePlay : handlePause}
                        style={{
                          background: COLORS.accent,
                          color: '#fff',
                          border: 'none',
                          borderRadius: '50%',
                          width: 36,
                          height: 36,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          cursor: isLoadingAudio ? 'wait' : 'pointer',
                          flexShrink: 0
                        }}
                        disabled={isLoadingAudio}
                        aria-label={isLoadingAudio ? "Loading audio" : !isPlaying ? "Play article audio" : "Pause article audio"}
                      >
                        {isLoadingAudio ? (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" style={{ animation: 'spin 1s linear infinite' }}>
                            <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
                          </svg>
                        ) : !isPlaying ? (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style={{ marginLeft: 2 }}><polygon points="5 3 19 12 5 21 5 3"/></svg>
                        ) : (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
                        )}
                      </button>

                      {/* Progress Bar & Time */}
                      <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 10 }}>
                        <input
                          type="range"
                          min="0"
                          max={duration || 100}
                          step="0.1"
                          value={currentTime}
                          onChange={handleSeek}
                          style={{
                            flex: 1,
                            cursor: isLoadingAudio || duration === 0 ? 'not-allowed' : 'pointer',
                            accentColor: COLORS.accent
                          }}
                          disabled={isLoadingAudio || duration === 0}
                        />
                        <div style={{ fontSize: 12, color: COLORS.textSec, fontVariantNumeric: 'tabular-nums', flexShrink: 0 }}>
                          {duration === 0 || isLoadingAudio ? "--:--" : formatTime(currentTime)} / {duration === 0 ? "--:--" : formatTime(duration)}
                        </div>
                      </div>
                    </div>
                  </>
                ) : (
                  <div style={{ marginBottom: 20, padding: '12px 16px', borderRadius: 12, border: `1px dashed ${COLORS.borderLight}`, background: COLORS.bg, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={COLORS.textTer} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="8" x2="12" y2="12" />
                      <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    <span style={{ fontSize: 13, color: COLORS.textTer, fontWeight: 500 }}>
                      Audio will be available soon
                    </span>
                  </div>
                )}

                {/* Article body (scroll-tracked) */}
                <ArticleBody
                  key={`${levelKey}:body`}
                  body={article.body}
                  vocabulary={article.vocabulary}
                  onVocabTap={setVocabSheet}
                  bodyRef={bodyRef}
                />

                {/* Source Citation & MVP Disclaimer */}
                {(article.sourcePublisher || article.sourceUrl) && (
                  <div style={{ marginTop: 32, padding: '16px 20px', background: COLORS.bg, borderRadius: 12, border: `1px solid ${COLORS.borderLight}` }}>
                    <p style={{ margin: '0 0 8px 0', fontSize: 13, color: COLORS.textSec, lineHeight: 1.5 }}>
                      This CurioFeed lesson is original English-learning content created from factual notes based on the source below.
                    </p>
                    <p style={{ margin: 0, fontSize: 13, color: COLORS.textSec, lineHeight: 1.5 }}>
                      Source:{' '}
                      {article.sourceUrl ? (
                        <a href={article.sourceUrl} target="_blank" rel="noopener noreferrer" style={{ color: COLORS.accent, textDecoration: 'none' }}>
                          {article.sourceTitle || article.title}
                        </a>
                      ) : (
                        <span>{article.sourceTitle || article.title}</span>
                      )}
                      {article.sourcePublisher && ` - Published by ${article.sourcePublisher}`}
                      {article.sourcePublishedAt && ` on ${formatDate(article.sourcePublishedAt)}`}
                    </p>
                  </div>
                )}

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
                  key={`${levelKey}:quiz`}
                  articleId={articleId}
                  quiz={article.quiz}
                  progress={progress}
                  onAnswer={onQuizAnswer}
                />
              </div>

              {/* Next article: shown after ≥1 quiz answered (UI_POLICY §10) */}
              {nextArticle && answeredCount >= 1 && (
                <NextArticleCard article={nextArticle} onTap={() => onNextArticle(nextArticle.id)} />
              )}
            </>
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
