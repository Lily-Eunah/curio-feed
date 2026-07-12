import { useMutation } from '@tanstack/react-query';
import StatusBadge from './StatusBadge';
import type { SubJobInfo } from '../api/types';
import { retrySubJob } from '../api/client';

interface SubJobTableProps {
  subJobs: SubJobInfo[];
  articleId?: string;
  jobId?: string;
  onRetrySuccess?: (level: string) => void;
}

function formatTime(dateStr?: string | null): string {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  } catch {
    return '—';
  }
}

export default function SubJobTable({
  subJobs,
  articleId,
  jobId,
  onRetrySuccess,
}: SubJobTableProps) {
  const canRetry = !!articleId && !!jobId;

  const retryMutation = useMutation({
    mutationFn: ({ subJobId }: { subJobId: string; level: string }) =>
      retrySubJob(articleId!, jobId!, subJobId),
    onSuccess: (_, variables) => {
      onRetrySuccess?.(variables.level);
    },
  });

  const retryingId = retryMutation.isPending ? retryMutation.variables?.subJobId : null;

  // Sort subjobs to ensure EASY, MEDIUM, HARD order
  const levelOrder = ['EASY', 'MEDIUM', 'HARD'];
  const sortedSubJobs = [...subJobs].sort(
    (a, b) => levelOrder.indexOf(a.level) - levelOrder.indexOf(b.level)
  );

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm text-gray-500">
        <thead>
          <tr className="border-b border-gray-200 text-xs font-semibold uppercase tracking-wider text-gray-400">
            <th className="pb-3 pt-2 font-medium">Level</th>
            <th className="pb-3 pt-2 font-medium">Status</th>
            <th className="pb-3 pt-2 font-medium">Retry Count</th>
            <th className="pb-3 pt-2 font-medium">Last Updated</th>
            {canRetry && <th className="pb-3 pt-2 font-medium text-right">Action</th>}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {sortedSubJobs.map((subJob) => {
            const isRetrying = retryingId === subJob.subJobId;

            return (
              <tr key={subJob.subJobId} className="hover:bg-gray-50/50 transition-colors">
                <td className="py-4 font-semibold text-gray-900">{subJob.level}</td>
                <td className="py-4">
                  <StatusBadge status={subJob.status} />
                </td>
                <td className="py-4 text-gray-700">{subJob.retryCount}</td>
                <td className="py-4 text-gray-500">
                  {formatTime(subJob.lastHeartbeatAt)}
                </td>
                {canRetry && (
                  <td className="py-4 text-right">
                    {subJob.status === 'FAILED' ? (
                      <button
                        onClick={() =>
                          retryMutation.mutate({
                            subJobId: subJob.subJobId,
                            level: subJob.level,
                          })
                        }
                        disabled={retryMutation.isPending}
                        className="rounded-md border border-red-200 px-3.5 py-1 text-xs font-semibold text-red-600 hover:bg-red-50 hover:border-red-300 disabled:cursor-not-allowed disabled:opacity-50 transition-all"
                      >
                        {isRetrying ? 'Retrying…' : 'Retry'}
                      </button>
                    ) : (
                      <span className="text-gray-400">—</span>
                    )}
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>

      {retryMutation.isError && (
        <p className="mt-3 text-xs font-medium text-red-600">
          Retry failed:{' '}
          {retryMutation.error instanceof Error
            ? retryMutation.error.message
            : 'Unexpected error. Please try again.'}
        </p>
      )}
    </div>
  );
}
