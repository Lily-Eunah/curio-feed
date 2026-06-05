export type StatusType =
  | 'PENDING'
  | 'PROCESSING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'DRAFT'
  | 'PUBLISHED'
  | 'ARCHIVED'
  | 'SKIPPED';

interface StatusConfig {
  label: string;
  className: string;
}

const STATUS_CONFIG: Record<StatusType, StatusConfig> = {
  PENDING: { label: 'Pending', className: 'bg-gray-100 text-gray-700' },
  PROCESSING: { label: 'Processing', className: 'bg-blue-100 text-blue-700' },
  RUNNING: { label: 'Running', className: 'bg-blue-100 text-blue-700' },
  COMPLETED: { label: 'Completed', className: 'bg-green-100 text-green-700' },
  FAILED: { label: 'Failed', className: 'bg-red-100 text-red-700' },
  DRAFT: { label: 'Draft', className: 'bg-gray-100 text-gray-700' },
  PUBLISHED: { label: 'Published', className: 'bg-green-100 text-green-700' },
  ARCHIVED: { label: 'Archived', className: 'bg-gray-100 text-gray-500' },
  SKIPPED: { label: 'Skipped', className: 'bg-gray-100 text-gray-400' },
};

interface StatusBadgeProps {
  status: StatusType | string;
  className?: string;
}

export default function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  const config = STATUS_CONFIG[status as StatusType];
  const label = config?.label ?? status;
  const colorClass = config?.className ?? 'bg-gray-100 text-gray-700';

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass} ${className}`}
    >
      {label}
    </span>
  );
}
