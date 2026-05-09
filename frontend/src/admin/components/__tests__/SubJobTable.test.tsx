import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SubJobTable from '../SubJobTable';
import { retrySubJob } from '../../api/client';
import { ApiError } from '../../../api/client';
import type { SubJobInfo } from '../../api/types';

vi.mock('../../api/client', () => ({
  retrySubJob: vi.fn(),
  retryStep: vi.fn(),
}));

const subJobs: SubJobInfo[] = [
  { subJobId: 'sub-1', level: 'EASY', status: 'COMPLETED', retryCount: 0, steps: [] },
  { subJobId: 'sub-2', level: 'MEDIUM', status: 'FAILED', retryCount: 2, steps: [] },
  { subJobId: 'sub-3', level: 'HARD', status: 'PENDING', retryCount: 0, steps: [] },
];

function renderTable(props: Partial<Parameters<typeof SubJobTable>[0]> = {}) {
  const qc = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <SubJobTable subJobs={subJobs} {...props} />
    </QueryClientProvider>,
  );
}

describe('SubJobTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(retrySubJob).mockResolvedValue(undefined);
  });

  it('renders a row for each sub-job', () => {
    renderTable();
    expect(screen.getByText('EASY')).toBeInTheDocument();
    expect(screen.getByText('MEDIUM')).toBeInTheDocument();
    expect(screen.getByText('HARD')).toBeInTheDocument();
  });

  describe('Retry button visibility', () => {
    it('does not show Retry button when articleId and jobId are not provided', () => {
      renderTable();
      expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument();
    });

    it('shows exactly one Retry all button for the FAILED row when retry context is provided', () => {
      renderTable({ articleId: 'art-1', jobId: 'job-1' });
      const buttons = screen.getAllByRole('button', { name: 'Retry all' });
      expect(buttons).toHaveLength(1);
    });

    it('does not show Retry all for COMPLETED or PENDING rows', () => {
      renderTable({ articleId: 'art-1', jobId: 'job-1' });
      // Only one Retry all button exists — it belongs to MEDIUM (FAILED)
      expect(screen.getAllByRole('button', { name: /retry/i })).toHaveLength(1);
    });
  });

  describe('Retry action', () => {
    it('calls retrySubJob with correct articleId, jobId, and subJobId', async () => {
      renderTable({ articleId: 'art-1', jobId: 'job-1' });

      fireEvent.click(screen.getByRole('button', { name: 'Retry all' }));

      await waitFor(() => {
        expect(retrySubJob).toHaveBeenCalledWith('art-1', 'job-1', 'sub-2');
      });
    });

    it('shows "Retrying…" on the button while the request is in flight', async () => {
      vi.mocked(retrySubJob).mockImplementation(() => new Promise(() => {}));
      renderTable({ articleId: 'art-1', jobId: 'job-1' });

      fireEvent.click(screen.getByRole('button', { name: 'Retry all' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Retrying…' })).toBeInTheDocument();
      });
    });

    it('disables the Retry button while the request is in flight', async () => {
      vi.mocked(retrySubJob).mockImplementation(() => new Promise(() => {}));
      renderTable({ articleId: 'art-1', jobId: 'job-1' });

      fireEvent.click(screen.getByRole('button', { name: 'Retry all' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Retrying…' })).toBeDisabled();
      });
    });

    it('calls onRetrySuccess after a successful retry', async () => {
      const onRetrySuccess = vi.fn();
      renderTable({ articleId: 'art-1', jobId: 'job-1', onRetrySuccess });

      fireEvent.click(screen.getByRole('button', { name: 'Retry all' }));

      await waitFor(() => {
        expect(onRetrySuccess).toHaveBeenCalledTimes(1);
      });
    });

    it('shows an inline error message on retry failure', async () => {
      vi.mocked(retrySubJob).mockRejectedValue(new Error('SubJob is not FAILED'));
      renderTable({ articleId: 'art-1', jobId: 'job-1' });

      fireEvent.click(screen.getByRole('button', { name: 'Retry all' }));

      await waitFor(() => {
        expect(screen.getByText(/retry failed/i)).toBeInTheDocument();
        expect(screen.getByText(/subjob is not failed/i)).toBeInTheDocument();
      });
    });

    it('shows error message from ApiError on retry failure', async () => {
      vi.mocked(retrySubJob).mockRejectedValue(
        new ApiError(400, { error: 'Bad Request', message: 'SubJob is not in FAILED state' }),
      );
      renderTable({ articleId: 'art-1', jobId: 'job-1' });

      fireEvent.click(screen.getByRole('button', { name: 'Retry all' }));

      await waitFor(() => {
        expect(screen.getByText(/subjob is not in failed state/i)).toBeInTheDocument();
      });
    });
  });
});
