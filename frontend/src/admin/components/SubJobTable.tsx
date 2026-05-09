import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import StatusBadge from './StatusBadge';
import type { SubJobInfo, StepJobInfo } from '../api/types';
import { retrySubJob, retryStep } from '../api/client';

interface SubJobTableProps {
  subJobs: SubJobInfo[];
  articleId?: string;
  jobId?: string;
  onRetrySuccess?: () => void;
}

const STEP_ORDER = ['CONTENT', 'VOCABULARY', 'QUIZ'] as const;

function StepRow({
  step,
  articleId,
  jobId,
  subJobId,
  onRetrySuccess,
}: {
  step: StepJobInfo;
  articleId: string;
  jobId: string;
  subJobId: string;
  onRetrySuccess?: () => void;
}) {
  const mutation = useMutation({
    mutationFn: () => retryStep(articleId, jobId, subJobId, step.stepType),
    onSuccess: () => onRetrySuccess?.(),
  });

  const durationMs = step.startedAt && step.completedAt 
    ? new Date(step.completedAt).getTime() - new Date(step.startedAt).getTime()
    : null;
  const durationStr = durationMs !== null ? ` (${(durationMs / 1000).toFixed(1)}s)` : '';

  const retryHint = step.stepType === 'CONTENT' 
    ? 'Resets VOCABULARY & QUIZ steps' 
    : step.stepType === 'VOCABULARY' 
      ? 'Resets QUIZ step' 
      : 'Resets QUIZ step only';

  const errors = step.validationErrors?.split(';').map(e => e.trim()).filter(Boolean) || [];

  return (
    <tr className="border-b border-gray-50 last:border-0 bg-gray-50/50">
      <td className="py-2 pl-8 text-xs font-mono text-gray-500">
        ↳ {step.stepType}
      </td>
      <td className="py-2">
        <StatusBadge status={step.status} />
      </td>
      <td className="py-2 text-xs text-gray-500 text-center">{step.attemptCount}</td>
      <td className="py-2 text-xs text-gray-400">
        {step.completedAt
          ? `${new Date(step.completedAt).toLocaleString()}${durationStr}`
          : step.startedAt
          ? `Started ${new Date(step.startedAt).toLocaleTimeString()}`
          : '-'}
      </td>
      <td className="py-2 text-xs">
        {errors.length > 0 && (
          <ul className="list-disc pl-4 text-orange-600 max-w-sm">
            {errors.map((err, i) => (
              <li key={i}>{err}</li>
            ))}
          </ul>
        )}
        {step.errorMessage && !step.validationErrors && (
          <span className="text-red-500 break-words max-w-sm block">
            {step.errorMessage}
          </span>
        )}
      </td>
      <td className="py-2 text-right">
        {step.status === 'FAILED' && (
          <div className="flex flex-col items-end gap-1">
            <button
              onClick={() => mutation.mutate()}
              disabled={mutation.isPending}
              title={retryHint}
              className="rounded px-2 py-1 text-xs font-medium text-red-600 hover:bg-red-50 border border-red-200 disabled:cursor-not-allowed disabled:opacity-50 transition-colors"
            >
              {mutation.isPending ? 'Retrying…' : 'Retry step'}
            </button>
            <span className="text-[10px] text-gray-400">{retryHint}</span>
          </div>
        )}
        {mutation.isError && (
          <span className="text-xs text-red-500 ml-1">Failed</span>
        )}
      </td>
    </tr>
  );
}

export default function SubJobTable({
  subJobs,
  articleId,
  jobId,
  onRetrySuccess,
}: SubJobTableProps) {
  const canRetry = !!articleId && !!jobId;
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const retryMutation = useMutation({
    mutationFn: (subJobId: string) => retrySubJob(articleId!, jobId!, subJobId),
    onSuccess: () => onRetrySuccess?.(),
  });

  const retryingId = retryMutation.isPending ? retryMutation.variables : null;

  const toggle = (id: string) =>
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  // Sort steps in canonical order
  const orderedSteps = (steps: StepJobInfo[]) =>
    STEP_ORDER.map((t) => steps.find((s) => s.stepType === t)).filter(
      Boolean,
    ) as StepJobInfo[];

  return (
    <>
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100 text-left text-xs font-medium text-gray-500">
            <th className="pb-2">Level / Step</th>
            <th className="pb-2">Status</th>
            <th className="pb-2">Tries</th>
            <th className="pb-2">Last activity</th>
            <th className="pb-2">Validation / Error</th>
            {canRetry && <th className="pb-2" />}
          </tr>
        </thead>
        <tbody>
          {subJobs.map((subJob) => {
            const isExpanded = expanded.has(subJob.subJobId);
            const hasSteps = subJob.steps && subJob.steps.length > 0;
            const isRetrying = retryingId === subJob.subJobId;

            return (
              <>
                {/* Level row */}
                <tr
                  key={subJob.subJobId}
                  className={`border-b border-gray-100 last:border-0 ${hasSteps ? 'cursor-pointer hover:bg-gray-50' : ''}`}
                  onClick={hasSteps ? () => toggle(subJob.subJobId) : undefined}
                >
                  <td className="py-3 font-medium text-gray-900">
                    {hasSteps && (
                      <span className="mr-1 text-gray-400 text-xs select-none">
                        {isExpanded ? '▾' : '▸'}
                      </span>
                    )}
                    {subJob.level}
                  </td>
                  <td className="py-3">
                    <StatusBadge status={subJob.status} />
                  </td>
                  <td className="py-3 text-gray-700">{subJob.retryCount}</td>
                  <td className="py-3 text-gray-500">
                    {subJob.lastHeartbeatAt
                      ? new Date(subJob.lastHeartbeatAt).toLocaleString()
                      : '-'}
                  </td>
                  <td className="py-3 text-gray-400 text-xs">
                    {hasSteps
                      ? `${subJob.steps.filter((s) => s.status === 'COMPLETED').length}/${subJob.steps.length} steps`
                      : ''}
                  </td>
                  {canRetry && (
                    <td className="py-3 text-right" onClick={(e) => e.stopPropagation()}>
                      {subJob.status === 'FAILED' && (
                        <button
                          onClick={() => retryMutation.mutate(subJob.subJobId)}
                          disabled={retryMutation.isPending}
                          className="rounded px-2 py-1 text-xs font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50 transition-colors"
                        >
                          {isRetrying ? 'Retrying…' : 'Retry all'}
                        </button>
                      )}
                    </td>
                  )}
                </tr>

                {/* Step rows (expanded) */}
                {isExpanded &&
                  hasSteps &&
                  orderedSteps(subJob.steps).map((step) => (
                    <StepRow
                      key={step.stepJobId}
                      step={step}
                      articleId={articleId!}
                      jobId={jobId!}
                      subJobId={subJob.subJobId}
                      onRetrySuccess={onRetrySuccess}
                    />
                  ))}
              </>
            );
          })}
        </tbody>
      </table>

      {retryMutation.isError && (
        <p className="mt-2 text-xs text-red-600">
          Retry failed:{' '}
          {retryMutation.error instanceof Error
            ? retryMutation.error.message
            : 'Unexpected error. Please try again.'}
        </p>
      )}
    </>
  );
}
