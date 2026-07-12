import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Card from '../components/Card';
import Button from '../components/Button';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import { getAdminArticleDetail, getGenerationStatus, updateAdminArticleStatus } from '../api/client';
import type { ArticleStatus, DifficultyLevel, AdminVocabInfo, AdminQuizInfo } from '../api/types';

const LEVEL_ORDER: DifficultyLevel[] = ['EASY', 'MEDIUM', 'HARD'];

// Vocab Tooltip Component for inline dotted underlines
function VocabTooltip({ entry, children }: { entry: AdminVocabInfo; children: React.ReactNode }) {
  const [visible, setVisible] = useState(false);

  return (
    <span
      className="relative inline-block border-b border-dashed border-indigo-500 text-indigo-700 font-semibold cursor-help transition-all"
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
    >
      {children}
      {visible && (
        <span className="absolute z-20 bottom-full left-1/2 transform -translate-x-1/2 mb-2 bg-slate-900 text-white text-[11px] font-medium rounded-lg px-2.5 py-1.5 shadow-lg whitespace-nowrap animate-[fadeInUp_0.15s_ease-out]">
          <span className="font-semibold text-indigo-300">{entry.word}</span>
          <span className="text-gray-400 mx-1">—</span>
          <span>{entry.definition}</span>
        </span>
      )}
    </span>
  );
}

// Body text parser that highlights vocab words
function ParsedBody({ text, vocabularies }: { text: string; vocabularies: AdminVocabInfo[] }) {
  if (!text) return null;
  const vocabMap = new Map(vocabularies.map((v) => [v.word.toLowerCase(), v]));

  // Sort by length descending to match longer phrases first
  const sortedWords = [...vocabularies].sort((a, b) => b.word.length - a.word.length);

  if (sortedWords.length === 0) {
    return (
      <div className="space-y-4 font-serif text-slate-800 leading-relaxed text-base sm:text-lg">
        {text.split('\n\n').map((para, i) => (
          <p key={i}>{para}</p>
        ))}
      </div>
    );
  }

  const patterns = sortedWords.map((v) => {
    const escaped = v.word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return `\\b${escaped}(?:s|es|d|ed|ing|ion|ions)?\\b`;
  });

  const combinedPattern = new RegExp(`({{[^}]+}})|(${patterns.join('|')})`, 'gi');
  const normalized = text.replace(/\\n/g, '\n');

  return (
    <div className="space-y-4 font-serif text-slate-800 leading-relaxed text-base sm:text-lg">
      {normalized.split('\n\n').map((para, pIdx) => {
        const parts = para.split(combinedPattern);
        const children = parts
          .filter((p) => p !== undefined && p !== '')
          .map((part, i) => {
            const markerMatch = part.match(/^{{(.+)}}$/);
            if (markerMatch) {
              const word = markerMatch[1];
              const entry = vocabMap.get(word.toLowerCase());
              if (entry) {
                return (
                  <VocabTooltip key={i} entry={entry}>
                    {word}
                  </VocabTooltip>
                );
              }
              return word;
            }

            const lowercasePart = part.toLowerCase();
            let entry = vocabMap.get(lowercasePart);
            if (!entry) {
              const base = sortedWords.find((v) => {
                const w = v.word.toLowerCase();
                return (
                  lowercasePart === w + 's' ||
                  lowercasePart === w + 'es' ||
                  lowercasePart === w + 'd' ||
                  lowercasePart === w + 'ed' ||
                  lowercasePart === w + 'ing' ||
                  lowercasePart === w + 'ion' ||
                  lowercasePart === w + 'ions'
                );
              });
              if (base) entry = base;
            }

            if (entry) {
              return (
                <VocabTooltip key={i} entry={entry}>
                  {part}
                </VocabTooltip>
              );
            }

            return part;
          });

        return <p key={pIdx}>{children}</p>;
      })}
    </div>
  );
}

// Parses JSON or split-semicolon options list
function parseQuizOptions(optionsStr: string): string[] {
  try {
    const parsed = JSON.parse(optionsStr);
    if (Array.isArray(parsed)) return parsed;
  } catch {
    // ignore and fallback
  }
  if (!optionsStr) return [];
  return optionsStr.split(';').map((s) => s.trim()).filter(Boolean);
}

// Renders individual quiz review items
function QuizReviewItem({ quiz, index }: { quiz: AdminQuizInfo; index: number }) {
  const options = parseQuizOptions(quiz.options);
  const correctVal = quiz.correctAnswer.trim();

  // Try parsing numeric index first
  const parsedIndex = parseInt(correctVal, 10);
  let correctIndex = isNaN(parsedIndex)
    ? correctVal.charCodeAt(0) - 65 // Handle 'A', 'B', 'C', 'D'
    : parsedIndex;

  const isMCQ = options.length > 0;

  return (
    <div className="border border-gray-150/70 rounded-xl p-4 bg-white/70 shadow-sm space-y-3">
      <div className="text-sm font-semibold text-slate-800">
        Q{index}. {quiz.question}
      </div>

      {isMCQ ? (
        <div className="space-y-2 pl-2">
          {options.map((opt, i) => {
            const isCorrect = i === correctIndex;
            return (
              <div key={i} className="flex items-center gap-2 text-xs sm:text-sm text-gray-700">
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center border transition-all ${
                    isCorrect
                      ? 'border-indigo-600 bg-indigo-50 text-indigo-600 ring-2 ring-indigo-600/10'
                      : 'border-gray-300 bg-gray-50'
                  }`}
                >
                  {isCorrect && (
                    <div className="w-1.5 h-1.5 rounded-full bg-indigo-600" />
                  )}
                </div>
                <span className={isCorrect ? 'font-medium text-slate-900' : ''}>
                  {String.fromCharCode(65 + i)}. {opt}
                </span>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="pl-2 space-y-1">
          <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">
            Model Answer
          </span>
          <p className="text-sm text-indigo-700 font-medium whitespace-pre-wrap">
            {quiz.correctAnswer}
          </p>
        </div>
      )}

      {quiz.explanation && (
        <div className="mt-2 text-xs text-gray-500 bg-gray-50 border border-gray-100 rounded-lg p-2.5 leading-relaxed">
          <span className="font-semibold text-gray-700 block mb-0.5">Explanation</span>
          {quiz.explanation}
        </div>
      )}
    </div>
  );
}

export default function ArticleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [selectedLevel, setSelectedLevel] = useState<DifficultyLevel>('EASY');
  const [showPublishModal, setShowPublishModal] = useState(false);
  const [showSuccessToast, setShowSuccessToast] = useState(false);
  const [expandedVocab, setExpandedVocab] = useState(false);

  const queryClient = useQueryClient();

  // Fetch article detail (for metadata + contents)
  const { data: detail, isLoading: detailLoading, isError: detailError, refetch: refetchDetail } = useQuery({
    queryKey: ['admin-article-detail', id],
    queryFn: () => getAdminArticleDetail(id!),
    enabled: !!id,
  });

  // Fetch status (for jobs / current overall status)
  const { data: statusData, refetch: refetchStatus } = useQuery({
    queryKey: ['article-detail', id],
    queryFn: () => getGenerationStatus(id!),
    enabled: !!id,
  });

  const statusMutation = useMutation({
    mutationFn: (newStatus: string) => updateAdminArticleStatus(id!, newStatus),
    onSuccess: () => {
      refetchDetail();
      refetchStatus();
      queryClient.invalidateQueries({ queryKey: ['adminArticles'] });
      setShowPublishModal(false);
      setShowSuccessToast(true);
    },
  });

  // Close toast automatically after 3 seconds
  useEffect(() => {
    if (showSuccessToast) {
      const timer = setTimeout(() => {
        setShowSuccessToast(false);
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [showSuccessToast]);

  const currentStatus = (detail?.status ?? statusData?.articleStatus) as ArticleStatus | undefined;
  const isPublished = currentStatus === 'PUBLISHED';

  const activeContent = detail?.contents?.find((c) => c.level === selectedLevel);

  const formatPublishDate = (dateStr?: string) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString();
  };

  const handlePublishClick = () => {
    if (isPublished) {
      // Archive if already published
      statusMutation.mutate('ARCHIVED');
    } else {
      setShowPublishModal(true);
    }
  };

  const handleConfirmPublish = () => {
    statusMutation.mutate('PUBLISHED');
  };

  const isLoading = detailLoading;
  const isError = detailError;

  return (
    <div className="space-y-6 max-w-6xl relative pb-12">
      {/* Top Header Card matching Review Content style */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Review Content</h1>
          <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-gray-500 font-medium">
            <span>{detail?.sourceName || 'Unknown Source'}</span>
            <span>•</span>
            <span>{detail?.categoryName || 'No Category'}</span>
            <span>•</span>
            <span className="text-xs text-gray-400 font-normal">
              Original published at {formatPublishDate(detail?.publishedAt || detail?.createdAt)}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Link to={`/admin/articles/${id}/status`}>
            <Button variant="secondary" className="px-4 py-2 font-semibold">
              Back to Status
            </Button>
          </Link>
          <Button
            variant={isPublished ? 'danger' : 'primary'}
            loading={statusMutation.isPending && !showPublishModal}
            onClick={handlePublishClick}
            className="px-5 py-2 font-semibold"
          >
            {isPublished ? 'Archive' : 'Publish'}
          </Button>
        </div>
      </div>

      {statusMutation.isError && (
        <div className="rounded-xl bg-red-50 p-4 border border-red-200 shadow-sm">
          <p className="text-sm text-red-700 font-medium">
            {(statusMutation.error as Error).message || 'Failed to update status'}
          </p>
        </div>
      )}

      {isLoading && <LoadingState message="Loading review content…" />}

      {isError && (
        <ErrorState
          title="Failed to load review content"
          message="Could not fetch article contents. Please check the pipeline status."
          onRetry={() => refetchDetail()}
        />
      )}

      {!isLoading && !isError && detail && (
        <>
          {/* Level Switcher Tabs */}
          <div className="border-b border-gray-200/80 bg-gray-50/50 p-1.5 rounded-xl inline-flex gap-2">
            {LEVEL_ORDER.map((lvl) => {
              const isActive = selectedLevel === lvl;
              const hasLvlContent = detail.contents?.some((c) => c.level === lvl);

              return (
                <button
                  key={lvl}
                  onClick={() => setSelectedLevel(lvl)}
                  disabled={!hasLvlContent}
                  className={`rounded-lg px-6 py-2 text-sm font-bold tracking-wide uppercase transition-all ${
                    isActive
                      ? 'bg-white text-indigo-600 shadow-sm border border-gray-200/50'
                      : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100/50 disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-gray-400'
                  }`}
                >
                  {lvl}
                </button>
              );
            })}
          </div>

          {activeContent ? (
            /* Two Column Review Grid */
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left Column: Parsed Article Title, Body & Quick Check */}
              <div className="lg:col-span-2 space-y-6">
                <Card className="p-6 sm:p-8">
                  <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight mb-6 font-serif">
                    {detail.title}
                  </h2>
                  <ParsedBody text={activeContent.content} vocabularies={activeContent.vocabularies} />
                </Card>

                {/* Quick Check Section */}
                <Card className="p-6 sm:p-8">
                  <h3 className="text-lg font-bold text-gray-900 mb-5 border-b border-gray-100 pb-3 uppercase tracking-wider">
                    Quick Check
                  </h3>
                  {activeContent.quizzes && activeContent.quizzes.length > 0 ? (
                    <div className="space-y-4">
                      {activeContent.quizzes.map((quiz, idx) => (
                        <QuizReviewItem key={quiz.id} quiz={quiz} index={idx + 1} />
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-gray-400 italic">No quizzes generated for this level.</p>
                  )}
                </Card>
              </div>

              {/* Right Column: Sidebar summaries */}
              <div className="space-y-6">
                {/* Vocab Summary Card */}
                <Card className="p-5">
                  <div className="flex justify-between items-center mb-4 pb-2 border-b border-gray-100">
                    <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider">
                      Vocab ({activeContent.vocabularies.length})
                    </h3>
                    <button
                      onClick={() => setExpandedVocab(!expandedVocab)}
                      className="text-xs font-semibold text-indigo-600 hover:underline"
                    >
                      {expandedVocab ? 'Collapse' : 'View all vocab'}
                    </button>
                  </div>
                  <ul className="space-y-3.5 divide-y divide-gray-50">
                    {activeContent.vocabularies
                      .slice(0, expandedVocab ? undefined : 6)
                      .map((v, i) => (
                        <li key={v.id} className={`text-sm ${i > 0 ? 'pt-2.5' : ''}`}>
                          <span className="font-bold text-indigo-600 block sm:inline-block">
                            {v.word}
                          </span>
                          <span className="hidden sm:inline text-gray-400 mx-1.5">/</span>
                          <span className="text-slate-700 font-medium block sm:inline">
                            {v.definition}
                          </span>
                          {v.exampleSentence && (
                            <p className="mt-1 text-xs text-gray-400 italic leading-relaxed">
                              "{v.exampleSentence}"
                            </p>
                          )}
                        </li>
                      ))}
                  </ul>
                </Card>

                {/* Quizzes Summary Card */}
                <Card className="p-5">
                  <div className="flex justify-between items-center mb-4 pb-2 border-b border-gray-100">
                    <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider">
                      Quiz ({activeContent.quizzes.length})
                    </h3>
                    <span className="text-xs font-medium text-gray-400">
                      {activeContent.quizzes.length} questions
                    </span>
                  </div>
                  <ul className="space-y-3">
                    {activeContent.quizzes.map((q, idx) => (
                      <li key={q.id} className="text-sm flex gap-2">
                        <span className="text-xs font-bold text-indigo-500 bg-indigo-50 rounded px-1.5 py-0.5 h-fit mt-0.5">
                          Q{idx + 1}
                        </span>
                        <div className="flex-1">
                          <p className="font-semibold text-slate-800 text-xs sm:text-sm leading-tight">
                            {q.question.length > 50 ? `${q.question.slice(0, 50)}...` : q.question}
                          </p>
                          <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wide block mt-0.5">
                            Type: {q.type.replace('_', ' ')}
                          </span>
                        </div>
                      </li>
                    ))}
                  </ul>
                </Card>
              </div>
            </div>
          ) : (
            <Card className="py-16 text-center">
              <p className="text-base font-bold text-gray-700">No content available for {selectedLevel}</p>
              <p className="mt-2 text-xs text-gray-400 max-w-md mx-auto leading-relaxed">
                The content and quizzes for this level haven't been generated yet or are currently in progress. Please check status below:
              </p>
              <Link to={`/admin/articles/${id}/status`} className="mt-4 inline-block">
                <Button variant="secondary" className="text-xs">
                  Check status
                </Button>
              </Link>
            </Card>
          )}
        </>
      )}

      {/* Publish Confirm Modal matching screenshot 07 */}
      {showPublishModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm transition-opacity duration-300">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl text-center border border-gray-100 transform scale-100 transition-all duration-300 animate-[fadeInUp_0.3s_ease-out]">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-50 text-green-500 mb-4 shadow-inner">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">Publish this article?</h3>
            <p className="text-sm text-gray-500 mb-6 leading-relaxed">
              This will make the EASY, MEDIUM, and HARD versions visible in the user feed.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowPublishModal(false)}
                className="flex-1 border border-gray-200 text-gray-700 font-semibold py-2.5 px-4 rounded-xl hover:bg-gray-50 transition-colors focus:outline-none"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmPublish}
                disabled={statusMutation.isPending}
                className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2.5 px-4 rounded-xl shadow-md shadow-indigo-600/10 transition-colors disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none"
              >
                {statusMutation.isPending ? 'Publishing…' : 'Publish'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Publish Success Toast matching screenshot 08 */}
      {showSuccessToast && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-white border border-gray-150 rounded-xl px-4 py-3.5 shadow-lg shadow-gray-200/80 max-w-sm animate-[fadeInUp_0.25s_ease-out]">
          <div className="flex items-center justify-center h-6 w-6 rounded-full bg-green-100 text-green-600 shrink-0">
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="3">
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-950">Article published</p>
            <p className="text-xs text-gray-500 mt-0.5 leading-snug">
              The article is now visible in the user feed.
            </p>
          </div>
          <button
            onClick={() => setShowSuccessToast(false)}
            className="text-gray-400 hover:text-gray-600 ml-2"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}
    </div>
  );
}
