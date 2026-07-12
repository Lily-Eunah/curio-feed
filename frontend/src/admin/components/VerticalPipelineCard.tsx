import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import StatusBadge from './StatusBadge';
import type { SubJobInfo, GenerationStepType } from '../api/types';
import { retrySubJob, retryStep } from '../api/client';
import Button from './Button';

interface VerticalPipelineCardProps {
  level: 'EASY' | 'MEDIUM' | 'HARD';
  subJob: SubJobInfo | undefined;
  articleId?: string;
  jobId?: string;
  onRetrySuccess?: (level: string, stepType?: string) => void;
}

const STEPS_ORDER: GenerationStepType[] = ['SOURCE_DIGEST', 'CONTENT', 'VOCABULARY', 'QUIZ'];

const STEP_LABELS: Record<GenerationStepType, string> = {
  SOURCE_DIGEST: 'Source Digest',
  CONTENT: 'Content Generation',
  VOCABULARY: 'Vocabulary',
  QUIZ: 'Quiz',
};

function formatTimeOnly(dateStr?: string | null): string {
  if (!dateStr) return '';
  try {
    return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  } catch {
    return '';
  }
}

function getDurationStr(startedAt: string | null | undefined, completedAt: string | null | undefined): string {
  if (!startedAt) return '';
  const start = new Date(startedAt).getTime();
  const end = completedAt ? new Date(completedAt).getTime() : Date.now();
  const diffSec = Math.max(0, Math.floor((end - start) / 1000));
  if (diffSec < 60) return `${diffSec}s`;
  const m = Math.floor(diffSec / 60);
  const s = diffSec % 60;
  return `${m}m ${s}s`;
}

export default function VerticalPipelineCard({
  level,
  subJob,
  articleId,
  jobId,
  onRetrySuccess,
}: VerticalPipelineCardProps) {
  const navigate = useNavigate();
  const canRetry = !!articleId && !!jobId;

  // Mutate for SubJob (when SOURCE_DIGEST fails or no specific step)
  const retrySubJobMutation = useMutation({
    mutationFn: () => retrySubJob(articleId!, jobId!, subJob!.subJobId),
    onSuccess: () => onRetrySuccess?.(level),
  });

  // Mutate for specific Step
  const retryStepMutation = useMutation({
    mutationFn: (stepType: GenerationStepType) => retryStep(articleId!, jobId!, subJob!.subJobId, stepType),
    onSuccess: (_, stepType) => onRetrySuccess?.(level, stepType),
  });

  const isAnyRetrying = retrySubJobMutation.isPending || retryStepMutation.isPending;

  const handleRetry = (stepType: GenerationStepType) => {
    if (stepType === 'SOURCE_DIGEST') {
      retrySubJobMutation.mutate();
    } else {
      retryStepMutation.mutate(stepType);
    }
  };

  const status = subJob?.status || 'PENDING';
  const lastHeartbeat = subJob?.lastHeartbeatAt;
  const allCompleted = status === 'COMPLETED';

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col h-full">
      {/* Header */}
      <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
        <div>
          <span className="text-sm font-bold text-gray-900 uppercase tracking-wider">{level}</span>
          <div className="text-xs text-gray-500 mt-1">
            {lastHeartbeat ? `Updated: ${formatTimeOnly(lastHeartbeat)}` : 'Pending'}
          </div>
        </div>
        <StatusBadge status={status} className="text-[10px] px-2 py-0.5" />
      </div>

      {/* Pipeline Body */}
      <div className="p-5 flex-1">
        <div className="flex flex-col">
          {STEPS_ORDER.map((stepType, index) => {
            const stepInfo = subJob?.steps?.find((s) => s.stepType === stepType);
            const stepStatus = stepInfo?.status || 'PENDING';
            const duration = getDurationStr(stepInfo?.startedAt, stepInfo?.completedAt);

            // Determine Icon
            let Icon = (
              <div className="w-5 h-5 rounded-full border-2 border-gray-200 bg-white" />
            );
            if (stepStatus === 'COMPLETED') {
              Icon = (
                <div className="w-5 h-5 rounded-full bg-green-500 flex items-center justify-center">
                  <svg className="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="3">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              );
            } else if (stepStatus === 'RUNNING' || stepStatus === 'PROCESSING') {
              Icon = (
                <div className="w-5 h-5 rounded-full border-2 border-blue-500 border-t-transparent animate-spin" />
              );
            } else if (stepStatus === 'FAILED') {
              Icon = (
                <div className="w-5 h-5 rounded-full bg-red-500 flex items-center justify-center">
                  <svg className="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="3">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </div>
              );
            }

            return (
              <div key={stepType} className="flex">
                {/* Left Column: Circle and Line */}
                <div className="flex flex-col items-center mr-4">
                  {/* Circle Icon */}
                  <div className="z-10 w-5 h-5 flex-shrink-0 bg-white rounded-full">
                    {Icon}
                  </div>
                  {/* Vertical Line connecting to next step */}
                  {index < STEPS_ORDER.length - 1 && (
                    <div 
                      className={`w-[2px] flex-1 ${
                        stepStatus === 'COMPLETED' ? 'bg-green-500' : 'bg-gray-100'
                      }`}
                    />
                  )}
                </div>

                {/* Content */}
                <div className="pb-6 flex-1 -mt-0.5">
                  <div className={`text-sm font-semibold ${stepStatus === 'PENDING' ? 'text-gray-400' : 'text-gray-900'}`}>
                    {STEP_LABELS[stepType]}
                  </div>
                  
                  {/* Status Subtext & Retry */}
                  <div className="mt-1 flex flex-col gap-2">
                    {stepStatus === 'COMPLETED' && (
                      <span className="text-xs text-gray-500 font-medium">Done in {duration}</span>
                    )}
                    {(stepStatus === 'RUNNING' || stepStatus === 'PROCESSING') && (
                      <span className="text-xs text-blue-600 font-medium animate-pulse">Running for {duration}...</span>
                    )}
                    {stepStatus === 'FAILED' && (
                      <div className="flex flex-col items-start gap-1.5">
                        <span className="text-xs text-red-500 font-medium">
                          Failed {stepInfo?.errorMessage ? `- ${stepInfo.errorMessage}` : ''}
                        </span>
                        {canRetry && (
                          <button
                            onClick={() => handleRetry(stepType)}
                            disabled={isAnyRetrying}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded text-[11px] font-bold border border-red-200 text-red-600 hover:bg-red-50 disabled:opacity-50 transition-colors"
                          >
                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            Retry Step
                          </button>
                        )}
                      </div>
                    )}
                    {stepStatus === 'PENDING' && (
                      <span className="text-xs text-gray-400">Pending</span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Footer / Actions */}
      <div className="p-4 bg-gray-50/50 border-t border-gray-100 flex justify-end">
        {allCompleted ? (
          <Button 
            variant="primary" 
            className="py-1.5 px-3 text-xs"
            onClick={() => navigate(`/admin/articles/${articleId}?level=${level}`)}
          >
            Review Content
          </Button>
        ) : (
          <span className="text-xs text-gray-400 font-medium px-2 py-1.5">
            {status === 'FAILED' ? 'Generation stopped' : 'Generation in progress...'}
          </span>
        )}
      </div>
      
      {/* Errors */}
      {(retrySubJobMutation.isError || retryStepMutation.isError) && (
        <div className="px-4 pb-4 bg-gray-50/50">
          <p className="text-xs font-medium text-red-600">
            Retry failed: {
              (retrySubJobMutation.error || retryStepMutation.error) instanceof Error 
                ? (retrySubJobMutation.error || retryStepMutation.error)?.message 
                : 'Unexpected error.'
            }
          </p>
        </div>
      )}
    </div>
  );
}
