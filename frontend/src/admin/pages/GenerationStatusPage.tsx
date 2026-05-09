import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import SubJobTable from '../components/SubJobTable';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import { getGenerationStatus } from '../api/client';
import type { GenerationStatusResponse } from '../api/types';

const ACTIVE_STATUSES = new Set(['PENDING', 'PROCESSING', 'RUNNING']);

function hasActiveSubJobs(data: GenerationStatusResponse | undefined): boolean {
  return !!data?.job?.subJobs.some((s) => ACTIVE_STATUSES.has(s.status));
}

export default function GenerationStatusPage() {
  const { articleId } = useParams<{ articleId: string }>();

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['generationStatus', articleId],
    queryFn: () => getGenerationStatus(articleId!),
    enabled: !!articleId,
    refetchInterval: (query) => (hasActiveSubJobs(query.state.data) ? 60_000 : false),
  });

  return (
    <div className="space-y-6 max-w-4xl">
      <PageHeader title="Generation Status">
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

      {data && (
        <>
          <Card>
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <dt className="text-xs font-medium text-gray-500">Article ID</dt>
                <dd className="mt-1 break-all font-mono text-sm text-gray-900">
                  {data.articleId}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500">Article Status</dt>
                <dd className="mt-1">
                  <StatusBadge status={data.articleStatus} />
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500">Job ID</dt>
                {data.job ? (
                  <dd className="mt-1 break-all font-mono text-sm text-gray-900">
                    {data.job.jobId}
                  </dd>
                ) : (
                  <dd className="mt-1 text-sm text-gray-400">No job created yet</dd>
                )}
              </div>
            </dl>
          </Card>

          {data.job ? (
            <Card>
              <h2 className="mb-4 text-sm font-semibold text-gray-700">Sub-Jobs</h2>
              <SubJobTable
                subJobs={data.job.subJobs}
                articleId={data.articleId}
                jobId={data.job.jobId}
                onRetrySuccess={() => refetch()}
              />
            </Card>
          ) : (
            <Card>
              <p className="text-sm text-gray-500">No job found for this article yet.</p>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
