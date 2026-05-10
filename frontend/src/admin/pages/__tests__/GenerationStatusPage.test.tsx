import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import GenerationStatusPage from '../GenerationStatusPage';
import { getGenerationStatus } from '../../api/client';
import type { GenerationStatusResponse } from '../../api/types';

vi.mock('../../api/client', () => ({
  getGenerationStatus: vi.fn(),
}));

function renderPage(articleId = 'article-123') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/admin/articles/${articleId}/status`]}>
        <Routes>
          <Route path="/admin/articles/:articleId/status" element={<GenerationStatusPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const mockStatus: GenerationStatusResponse = {
  articleId: 'article-123',
  articleStatus: 'REVIEWING',
  job: {
    jobId: 'job-456',
    subJobs: [
      { subJobId: 'sub-1', level: 'EASY', status: 'COMPLETED', retryCount: 0, steps: [] },
      { subJobId: 'sub-2', level: 'MEDIUM', status: 'PROCESSING', retryCount: 1, steps: [] },
      { subJobId: 'sub-3', level: 'HARD', status: 'PENDING', retryCount: 0, steps: [] },
    ],
  },
};

describe('GenerationStatusPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getGenerationStatus).mockResolvedValue(mockStatus);
  });

  it('shows page heading immediately', () => {
    vi.mocked(getGenerationStatus).mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('heading', { name: /generation status/i })).toBeInTheDocument();
  });

  it('shows loading state before data arrives', () => {
    vi.mocked(getGenerationStatus).mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getAllByText(/loading/i).length).toBeGreaterThan(0);
  });

  it('renders article id, status badge, and job id after load', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('article-123')).toBeInTheDocument();
      expect(screen.getByText(/reviewing/i)).toBeInTheDocument();
      expect(screen.getByText('job-456')).toBeInTheDocument();
    });
  });

  it('renders a row for each sub-job level', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('EASY')).toBeInTheDocument();
      expect(screen.getByText('MEDIUM')).toBeInTheDocument();
      expect(screen.getByText('HARD')).toBeInTheDocument();
    });
  });

  it('shows retry count for each sub-job', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument(); // MEDIUM retryCount
      const zeros = screen.getAllByText('0');
      expect(zeros.length).toBeGreaterThanOrEqual(2); // EASY and HARD retryCount
    });
  });

  it('shows "-" for lastHeartbeatAt when absent from backend DTO', async () => {
    renderPage();
    await waitFor(() => {
      const dashes = screen.getAllByText('-');
      expect(dashes.length).toBeGreaterThanOrEqual(3);
    });
  });

  it('renders Refresh Now button', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /refresh now/i })).toBeInTheDocument();
  });

  it('calls getGenerationStatus again when Refresh Now is clicked', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('article-123')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /refresh now/i }));

    await waitFor(() => {
      expect(getGenerationStatus).toHaveBeenCalledTimes(2);
    });
  });

  it('shows error state on API failure', async () => {
    vi.mocked(getGenerationStatus).mockRejectedValue(new Error('Network error'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
    });
  });

  it('shows "no job" message when job is null', async () => {
    vi.mocked(getGenerationStatus).mockResolvedValue({ ...mockStatus, job: null });
    renderPage();
    await waitFor(() => {
      // Two elements match /no job/i (dd + p), so match the card paragraph exactly
      expect(
        screen.getByText('No job found for this article yet.'),
      ).toBeInTheDocument();
    });
  });
});
