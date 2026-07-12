import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import VerticalPipelineCard from '../components/VerticalPipelineCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import { getGenerationStatus, getAdminArticleDetail } from '../api/client';
import type { GenerationStatusResponse } from '../api/types';

const ACTIVE_STATUSES = new Set(['PENDING', 'PROCESSING', 'RUNNING']);

function hasActiveSubJobs(data: GenerationStatusResponse | undefined): boolean {
  return !!data?.job?.subJobs.some((s) => ACTIVE_STATUSES.has(s.status));
}

function getOverallStatus(data: GenerationStatusResponse | undefined) {
  if (!data?.job) return data?.articleStatus || 'PENDING';
  const subJobs = data.job.subJobs;
  const failedSubJob = subJobs.find((s) => s.status === 'FAILED');
  if (failedSubJob) {
    return `FAILED (${failedSubJob.retryCount})`;
  }
  const isRunning = subJobs.some((s) => s.status === 'RUNNING' || s.status === 'PROCESSING');
  if (isRunning) return 'PROCESSING';
  const allCompleted = subJobs.every((s) => s.status === 'COMPLETED');
  if (allCompleted) return 'COMPLETED';
  return 'PENDING';
}


export default function GenerationStatusPage() {
  const { articleId } = useParams<{ articleId: string }>();
  const [retryModalLevel, setRetryModalLevel] = useState<string | null>(null);

  // 1. Fetch Generation Status
  const { data: statusData, isLoading: statusLoading, isError: statusError, refetch, isFetching } = useQuery({
    queryKey: ['generationStatus', articleId],
    queryFn: () => getGenerationStatus(articleId!),
    enabled: !!articleId,
    refetchInterval: (query) => (hasActiveSubJobs(query.state.data) ? 10_000 : false), // Poll faster (10s) when active
  });

  // 2. Fetch Article Details for Title and Meta
  const { data: detailData, isLoading: detailLoading } = useQuery({
    queryKey: ['admin-article-detail', articleId],
    queryFn: () => getAdminArticleDetail(articleId!),
    enabled: !!articleId,
  });

  const overallStatus = getOverallStatus(statusData);

  const formatCheckedTime = () => {
    return new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  };


  const isError = statusError;
  const isLoading = statusLoading || detailLoading;

  return (
    <div className="space-y-6 max-w-4xl relative">
      <PageHeader title="Generation Status" description="AI generation usually takes 10-30 minutes.">
        <Button variant="secondary" loading={isFetching} onClick={() => refetch()}>
          Refresh Now
        </Button>
      </PageHeader>

      {isLoading && <LoadingState />}

      {isError && (
        <ErrorState
          title="Failed to load generation status"
          message="Could not fetch the generation status for this article."
          onRetry={() => refetch()}
        />
      )}

      {!isLoading && !isError && statusData && (
        <>
          {/* Article Info Header Card */}
          <Card>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div>
                <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block mb-1">Article</span>
                <h2 className="text-xl font-bold text-gray-900 leading-tight">
                  {detailData?.title || detailData?.originalTitle || `Article ${articleId}`}
                </h2>
                <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-gray-500 font-medium">
                  <span>{detailData?.sourceName || 'Unknown Source'}</span>
                  <span>•</span>
                  <span>{detailData?.categoryName || 'No Category'}</span>
                  <span>•</span>
                  <span className="text-xs text-gray-400 font-normal">
                    Created At:{' '}
                    {detailData?.createdAt
                      ? new Date(detailData.createdAt).toLocaleString()
                      : '—'}
                  </span>
                </div>
              </div>
              <div className="flex flex-col sm:items-end gap-1">
                <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block sm:text-right">
                  Overall Status
                </span>
                <div className="mt-0.5">
                  <StatusBadge
                    status={overallStatus.startsWith('FAILED') ? 'FAILED' : overallStatus}
                    className="text-xs px-3 py-1 font-bold"
                  />
                  {overallStatus.startsWith('FAILED') && (
                    <span className="ml-2 text-xs font-bold text-red-600">{overallStatus}</span>
                  )}
                </div>
                <span className="text-[11px] text-gray-400 mt-0.5 sm:text-right font-medium">
                  Last checked: {formatCheckedTime()}
                </span>
              </div>
            </div>
          </Card>

          {/* Vertical Pipeline Steppers */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {(['EASY', 'MEDIUM', 'HARD'] as const).map((level) => {
              const subJob = statusData.job?.subJobs.find((s) => s.level === level);
              return (
                <VerticalPipelineCard
                  key={level}
                  level={level}
                  subJob={subJob}
                  articleId={statusData.articleId}
                  jobId={statusData.job?.jobId}
                  onRetrySuccess={(retryLevel) => {
                    setRetryModalLevel(retryLevel);
                    refetch();
                  }}
                />
              );
            })}
          </div>
        </>
      )}

      {/* Retry Confirmation Modal */}
      {retryModalLevel && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm transition-opacity duration-300">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl text-center border border-gray-100 transform scale-100 transition-all duration-300 animate-[fadeInUp_0.3s_ease-out]">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100 text-green-600 mb-4 shadow-inner">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">Retry requested</h3>
            <p className="text-sm text-gray-600 mb-1 leading-relaxed">
              <span className="font-semibold text-indigo-600">{retryModalLevel}</span> level generation retry has been requested successfully.
            </p>
            <p className="text-xs text-gray-400 mb-6 font-medium">
              Status will refresh automatically in 1 minute.
            </p>
            <button
              onClick={() => setRetryModalLevel(null)}
              className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2.5 px-4 rounded-xl shadow-md shadow-indigo-600/10 transition-colors focus:outline-none"
            >
              OK
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
