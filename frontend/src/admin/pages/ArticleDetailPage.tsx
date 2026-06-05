import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import SubJobTable from '../components/SubJobTable';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import { getAdminArticleDetail, getGenerationStatus, updateAdminArticleStatus } from '../api/client';
import type { ArticleStatus, AdminContentInfo } from '../api/types';

type Tab = 'overview' | 'content' | 'jobs';

const TABS: { id: Tab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'content', label: 'Content' },
  { id: 'jobs', label: 'Jobs' },
];

function getStatusAction(status: ArticleStatus | undefined): {
  label: string;
  targetStatus: string;
  variant: 'primary' | 'danger';
} | null {
  switch (status) {
    case 'DRAFT':
    case 'ARCHIVED':
      return { label: 'Publish', targetStatus: 'PUBLISHED', variant: 'primary' };
    case 'PUBLISHED':
      return { label: 'Archive', targetStatus: 'ARCHIVED', variant: 'danger' };
    default:
      return null;
  }
}

function MetaRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="py-3 sm:grid sm:grid-cols-3 sm:gap-4">
      <dt className="text-sm font-medium text-gray-500">{label}</dt>
      <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">{children}</dd>
    </div>
  );
}

const LEVEL_ORDER = ['EASY', 'MEDIUM', 'HARD'];

function ContentPanel({ content }: { content: AdminContentInfo }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden mb-3">
      <button
        className="w-full flex items-center justify-between px-4 py-3 bg-gray-50 text-left"
        onClick={() => setOpen(o => !o)}
      >
        <span className="text-sm font-semibold text-gray-700">{content.level}</span>
        <svg
          className={`w-4 h-4 text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`}
          fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && (
        <div className="px-4 py-4 space-y-4">
          <div>
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Body</p>
            <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">{content.content}</p>
          </div>
          {content.vocabularies.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Vocabulary ({content.vocabularies.length})
              </p>
              <ul className="space-y-1">
                {content.vocabularies.map(v => (
                  <li key={v.id} className="text-sm">
                    <span className="font-medium text-indigo-700">{v.word}</span>
                    <span className="text-gray-500"> — {v.definition}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {content.quizzes.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Quizzes ({content.quizzes.length})
              </p>
              <ul className="space-y-2">
                {content.quizzes.map((q, i) => (
                  <li key={q.id} className="text-sm border border-gray-100 rounded p-2">
                    <span className="font-medium text-gray-700">Q{i + 1} [{q.type}]</span>{' '}
                    <span className="text-gray-600">{q.question}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function ArticleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const queryClient = useQueryClient();

  const { data: detail, isLoading: detailLoading, isError: detailError, refetch: refetchDetail } = useQuery({
    queryKey: ['admin-article-detail', id],
    queryFn: () => getAdminArticleDetail(id!),
    enabled: !!id,
  });

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
    },
  });

  const currentStatus = (detail?.status ?? statusData?.articleStatus) as ArticleStatus | undefined;
  const statusAction = getStatusAction(currentStatus);

  const sortedContents = [...(detail?.contents ?? [])].sort(
    (a, b) => LEVEL_ORDER.indexOf(a.level) - LEVEL_ORDER.indexOf(b.level),
  );

  return (
    <div className="space-y-6 max-w-4xl">
      <PageHeader title={detail?.title ?? `Article ${id}`} description="Article details and generation status">
        {statusAction && (
          <Button
            variant={statusAction.variant}
            loading={statusMutation.isPending}
            onClick={() => statusMutation.mutate(statusAction.targetStatus)}
          >
            {statusAction.label}
          </Button>
        )}
        <Link to={`/admin/articles/${id}/status`}>
          <Button variant="secondary">Full Status Page</Button>
        </Link>
      </PageHeader>

      {statusMutation.isError && (
        <div className="rounded-md bg-red-50 p-4 border border-red-200">
          <p className="text-sm text-red-700">
            {(statusMutation.error as Error).message || 'Failed to update status'}
          </p>
        </div>
      )}

      {detailLoading && <LoadingState message="Loading article…" />}

      {detailError && (
        <ErrorState
          title="Failed to load article"
          message="Could not fetch article details. Please try again."
          onRetry={() => refetchDetail()}
        />
      )}

      {!detailLoading && !detailError && (
        <>
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex gap-6">
              {TABS.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`border-b-2 pb-3 text-sm font-medium transition-colors ${
                    activeTab === tab.id
                      ? 'border-indigo-600 text-indigo-600'
                      : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </nav>
          </div>

          {activeTab === 'overview' && (
            <Card>
              <dl className="divide-y divide-gray-100">
                <MetaRow label="Article ID">
                  <span className="break-all font-mono">{detail?.id ?? id ?? '—'}</span>
                </MetaRow>
                <MetaRow label="Status">
                  {currentStatus ? (
                    <StatusBadge status={currentStatus} />
                  ) : (
                    <span className="text-gray-400">—</span>
                  )}
                </MetaRow>
                <MetaRow label="Job ID">
                  {statusData?.job ? (
                    <span className="break-all font-mono">{statusData.job.jobId}</span>
                  ) : (
                    <span className="text-gray-400">No job created yet</span>
                  )}
                </MetaRow>
                <MetaRow label="Title">
                  {detail?.title ?? <span className="text-gray-400">—</span>}
                </MetaRow>
                <MetaRow label="Original Title">
                  {detail?.originalTitle ?? <span className="text-gray-400">—</span>}
                </MetaRow>
                <MetaRow label="Source">
                  {detail?.sourceName ? (
                    <span>
                      {detail.sourceName}
                      {detail.sourceUrl && (
                        <a
                          href={detail.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="ml-2 text-indigo-600 hover:underline text-xs"
                        >
                          ↗ Link
                        </a>
                      )}
                    </span>
                  ) : (
                    <span className="text-gray-400">—</span>
                  )}
                </MetaRow>
                <MetaRow label="Category">
                  {detail?.categoryName ?? <span className="text-gray-400">—</span>}
                </MetaRow>
                <MetaRow label="Published">
                  {detail?.publishedAt
                    ? new Date(detail.publishedAt).toLocaleString()
                    : <span className="text-gray-400">—</span>}
                </MetaRow>
              </dl>
            </Card>
          )}

          {activeTab === 'content' && (
            <Card>
              {sortedContents.length > 0 ? (
                <div>
                  <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
                    {sortedContents.length} level{sortedContents.length !== 1 ? 's' : ''} available
                  </p>
                  {sortedContents.map(c => (
                    <ContentPanel key={c.id} content={c} />
                  ))}
                </div>
              ) : (
                <div className="py-12 text-center">
                  <p className="text-sm font-medium text-gray-700">No content available</p>
                  <p className="mt-1 text-xs text-gray-400">
                    Content is generated after the article is registered and processed.
                  </p>
                </div>
              )}
            </Card>
          )}

          {activeTab === 'jobs' && (
            <Card>
              {statusData?.job ? (
                <>
                  <h2 className="mb-4 text-sm font-semibold text-gray-700">Sub-Jobs</h2>
                  <SubJobTable
                    subJobs={statusData.job.subJobs}
                    articleId={statusData?.articleId}
                    jobId={statusData?.job?.jobId}
                    onRetrySuccess={() => refetchStatus()}
                  />
                </>
              ) : (
                <p className="py-8 text-center text-sm text-gray-500">
                  No generation job found for this article.
                </p>
              )}
            </Card>
          )}
        </>
      )}
    </div>
  );
}
