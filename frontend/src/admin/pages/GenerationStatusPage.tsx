import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import SubJobTable from '../components/SubJobTable';
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

function getLevelProgressInfo(subJobs: any[], level: 'EASY' | 'MEDIUM' | 'HARD') {
  const subJob = subJobs.find((s) => s.level === level);
  if (!subJob) {
    return { status: 'PENDING', retryCount: 0 };
  }
  return { status: subJob.status, retryCount: subJob.retryCount };
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

  const subJobs = statusData?.job?.subJobs || [];
  const overallStatus = getOverallStatus(statusData);

  const formatCheckedTime = () => {
    return new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  };

  const renderProgressCircle = (level: 'EASY' | 'MEDIUM' | 'HARD') => {
    const { status, retryCount } = getLevelProgressInfo(subJobs, level);

    if (status === 'COMPLETED') {
      return (
        <div className="flex flex-col items-center justify-center">
          <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2.5">{level}</span>
          <div className="flex items-center justify-center w-14 h-14 rounded-full bg-green-50 text-green-500 border-2 border-green-500 shadow-sm mb-2">
            <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="3">
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <span className="text-xs font-bold text-green-600 uppercase tracking-wide">Completed</span>
          <span className="text-[11px] text-gray-400 mt-1">Retry Count {retryCount}</span>
        </div>
      );
    }

    if (status === 'RUNNING' || status === 'PROCESSING') {
      return (
        <div className="flex flex-col items-center justify-center">
          <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2.5">{level}</span>
          <div className="flex items-center justify-center w-14 h-14 rounded-full bg-blue-50 text-blue-500 border-2 border-blue-500 shadow-sm mb-2 relative">
            <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
          <span className="text-xs font-bold text-blue-600 uppercase tracking-wide">Running</span>
          <span className="text-[11px] text-gray-400 mt-1">Retry Count {retryCount}</span>
        </div>
      );
    }

    if (status === 'FAILED') {
      return (
        <div className="flex flex-col items-center justify-center">
          <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2.5">{level}</span>
          <div className="flex items-center justify-center w-14 h-14 rounded-full bg-red-50 text-red-500 border-2 border-red-500 shadow-sm mb-2">
            <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="3">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <span className="text-xs font-bold text-red-600 uppercase tracking-wide">Failed</span>
          <span className="text-[11px] text-gray-400 mt-1">Retry Count {retryCount}</span>
        </div>
      );
    }

    // PENDING / READY
    return (
      <div className="flex flex-col items-center justify-center">
        <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2.5">{level}</span>
        <div className="flex items-center justify-center w-14 h-14 rounded-full bg-gray-50 text-gray-400 border-2 border-gray-200 shadow-sm mb-2">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <span className="text-xs font-bold text-gray-400 uppercase tracking-wide">Pending</span>
        <span className="text-[11px] text-gray-400 mt-1">Retry Count {retryCount}</span>
      </div>
    );
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

          {/* Generation Progress Card */}
          <Card>
            <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider mb-6">
              Generation Progress
            </h3>
            <div className="grid grid-cols-3 gap-4 border-t border-b border-gray-100 py-6 my-2 bg-gray-50/30 rounded-lg">
              {renderProgressCircle('EASY')}
              {renderProgressCircle('MEDIUM')}
              {renderProgressCircle('HARD')}
            </div>
          </Card>

          {/* Sub Jobs Table Card */}
          <Card>
            <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider mb-4">
              Sub Jobs
            </h3>
            {statusData.job ? (
              <SubJobTable
                subJobs={statusData.job.subJobs}
                articleId={statusData.articleId}
                jobId={statusData.job.jobId}
                onRetrySuccess={(level) => {
                  setRetryModalLevel(level);
                  refetch();
                }}
              />
            ) : (
              <div className="py-6 text-center text-sm text-gray-400 font-medium">
                No active background jobs found for this article.
              </div>
            )}
          </Card>
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
