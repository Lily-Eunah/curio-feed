import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ArticleDetailPage from '../ArticleDetailPage';
import { getGenerationStatus, getAdminArticleDetail, updateAdminArticleStatus } from '../../api/client';
import type { GenerationStatusResponse, AdminArticleDetailResponse } from '../../api/types';

vi.mock('../../api/client', () => ({
  getGenerationStatus: vi.fn(),
  getAdminArticleDetail: vi.fn(),
  updateAdminArticleStatus: vi.fn(),
  retrySubJob: vi.fn(),
  retryStep: vi.fn(),
}));

function renderPage(articleId = 'article-abc') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/admin/articles/${articleId}`]}>
        <Routes>
          <Route path="/admin/articles/:id" element={<ArticleDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const mockStatus: GenerationStatusResponse = {
  articleId: 'article-abc',
  articleStatus: 'REVIEWING',
  job: {
    jobId: 'job-xyz',
    subJobs: [
      { subJobId: 'sub-1', level: 'EASY', status: 'COMPLETED', retryCount: 0, steps: [] },
      { subJobId: 'sub-2', level: 'MEDIUM', status: 'PROCESSING', retryCount: 2, steps: [] },
      { subJobId: 'sub-3', level: 'HARD', status: 'PENDING', retryCount: 0, steps: [] },
    ],
  },
};

const mockDetail: AdminArticleDetailResponse = {
  id: 'article-abc',
  status: 'REVIEWING',
  title: null as unknown as string,    // missing — renders as "—"
  originalTitle: null as unknown as string,
  sourceName: null as unknown as string,
  sourceUrl: null as unknown as string,
  categoryId: null,
  categoryName: null,
  originalContent: null,
  createdAt: '2026-05-01T10:00:00Z',
  publishedAt: null as unknown as string,
  job: { jobId: 'job-xyz', status: 'PROCESSING' },
  contents: [],
};

describe('ArticleDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getGenerationStatus).mockResolvedValue(mockStatus);
    vi.mocked(getAdminArticleDetail).mockResolvedValue(mockDetail);
    vi.mocked(updateAdminArticleStatus).mockResolvedValue({
      articleId: 'article-abc',
      status: 'PUBLISHED',
    });
  });

  it('shows loading state while data is fetching', () => {
    vi.mocked(getGenerationStatus).mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getAllByText(/loading/i).length).toBeGreaterThan(0);
  });

  it('shows error state with retry button on API failure', async () => {
    vi.mocked(getAdminArticleDetail).mockRejectedValue(new Error('Network error'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/failed to load article/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
    });
  });

  it('calls getAdminArticleDetail again when retry is clicked', async () => {
    vi.mocked(getAdminArticleDetail).mockRejectedValue(new Error('Network error'));
    renderPage();
    await waitFor(() => screen.getByRole('button', { name: /try again/i }));

    fireEvent.click(screen.getByRole('button', { name: /try again/i }));

    await waitFor(() => {
      expect(getAdminArticleDetail).toHaveBeenCalledTimes(2);
    });
  });

  it('renders page heading with article id', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /article article-abc/i })).toBeInTheDocument();
    });
  });

  it('shows Full Status Page link', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByRole('link', { name: /full status page/i })).toBeInTheDocument();
    });
  });

  describe('Overview tab (default)', () => {
    it('shows article ID, status badge, and job ID', async () => {
      renderPage();
      await waitFor(() => {
        expect(screen.getByText('article-abc')).toBeInTheDocument();
        expect(screen.getByText(/reviewing/i)).toBeInTheDocument();
        expect(screen.getByText('job-xyz')).toBeInTheDocument();
      });
    });

    it('shows em-dash placeholders for missing title, source, and category', async () => {
      renderPage();
      await waitFor(() => {
        // Component renders "—" for null title, originalTitle, source, category, published
        const dashes = screen.getAllByText('—');
        expect(dashes.length).toBeGreaterThanOrEqual(3);
      });
    });

    it('shows "No job created yet" when job is null', async () => {
      vi.mocked(getGenerationStatus).mockResolvedValue({ ...mockStatus, job: null });
      renderPage();
      await waitFor(() => {
        expect(screen.getByText('No job created yet')).toBeInTheDocument();
      });
    });
  });

  describe('Content tab', () => {
    it('shows "No content available" placeholder after clicking Content tab', async () => {
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: 'Content' }));

      fireEvent.click(screen.getByRole('button', { name: 'Content' }));

      await waitFor(() => {
        expect(screen.getByText(/no content available/i)).toBeInTheDocument();
      });
    });
  });

  describe('Jobs tab', () => {
    it('shows sub-job rows after clicking Jobs tab', async () => {
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: 'Jobs' }));

      fireEvent.click(screen.getByRole('button', { name: 'Jobs' }));

      expect(screen.getByText('EASY')).toBeInTheDocument();
      expect(screen.getByText('MEDIUM')).toBeInTheDocument();
      expect(screen.getByText('HARD')).toBeInTheDocument();
    });

    it('shows retry count in Jobs tab', async () => {
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: 'Jobs' }));

      fireEvent.click(screen.getByRole('button', { name: 'Jobs' }));

      expect(screen.getByText('2')).toBeInTheDocument(); // MEDIUM retryCount
    });

    it('shows no-job message in Jobs tab when job is null', async () => {
      vi.mocked(getGenerationStatus).mockResolvedValue({ ...mockStatus, job: null });
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: 'Jobs' }));

      fireEvent.click(screen.getByRole('button', { name: 'Jobs' }));

      expect(screen.getByText(/no generation job found/i)).toBeInTheDocument();
    });
  });

  describe('Status controls', () => {
    it('shows Publish button when article is in REVIEWING status', async () => {
      renderPage();
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /publish/i })).toBeInTheDocument();
      });
    });

    it('shows Publish button when article is in DRAFT status', async () => {
      vi.mocked(getGenerationStatus).mockResolvedValue({
        ...mockStatus,
        articleStatus: 'DRAFT',
      });
      renderPage();
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /publish/i })).toBeInTheDocument();
      });
    });

    it('shows Hide button when article is PUBLISHED', async () => {
      vi.mocked(getAdminArticleDetail).mockResolvedValue({ ...mockDetail, status: 'PUBLISHED' });
      renderPage();
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /hide/i })).toBeInTheDocument();
      });
    });

    it('shows Publish button when article is HIDDEN', async () => {
      vi.mocked(getGenerationStatus).mockResolvedValue({
        ...mockStatus,
        articleStatus: 'HIDDEN',
      });
      renderPage();
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /publish/i })).toBeInTheDocument();
      });
    });

    it('calls updateAdminArticleStatus with PUBLISHED when Publish is clicked', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: /publish/i }));

      await user.click(screen.getByRole('button', { name: /publish/i }));

      await waitFor(() => {
        expect(updateAdminArticleStatus).toHaveBeenCalledWith('article-abc', 'PUBLISHED');
      });
    });

    it('calls updateAdminArticleStatus with HIDDEN when Hide is clicked', async () => {
      const user = userEvent.setup();
      vi.mocked(getAdminArticleDetail).mockResolvedValue({ ...mockDetail, status: 'PUBLISHED' });
      vi.mocked(updateAdminArticleStatus).mockResolvedValue({
        articleId: 'article-abc',
        status: 'HIDDEN',
      });
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: /hide/i }));

      await user.click(screen.getByRole('button', { name: /hide/i }));

      await waitFor(() => {
        expect(updateAdminArticleStatus).toHaveBeenCalledWith('article-abc', 'HIDDEN');
      });
    });

    it('refetches article detail after successful status update', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: /publish/i }));

      await user.click(screen.getByRole('button', { name: /publish/i }));

      await waitFor(() => {
        // Initial fetch + refetch after mutation
        expect(getGenerationStatus).toHaveBeenCalledTimes(2);
      });
    });

    it('shows error message on status update failure', async () => {
      const user = userEvent.setup();
      vi.mocked(updateAdminArticleStatus).mockRejectedValue(
        new Error('Invalid status transition'),
      );
      renderPage();
      await waitFor(() => screen.getByRole('button', { name: /publish/i }));

      await user.click(screen.getByRole('button', { name: /publish/i }));

      await waitFor(() => {
        expect(screen.getByText(/invalid status transition/i)).toBeInTheDocument();
      });
    });
  });
});
