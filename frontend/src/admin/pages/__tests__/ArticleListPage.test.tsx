import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ArticleListPage from '../ArticleListPage';
import { getAdminArticles } from '../../api/client';
import type { AdminArticleListPage } from '../../api/types';

vi.mock('../../api/client', () => ({
  getAdminArticles: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/admin/articles']}>
        <ArticleListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const mockPageData: AdminArticleListPage = {
  content: [
    {
      id: 'art-1',
      originalTitle: 'AI Revolution',
      sourceName: 'TechCrunch',
      status: 'DRAFT',
      categoryName: 'Technology',
      createdAt: '2026-04-27T10:00:00Z',
    },
    {
      id: 'art-2',
      originalTitle: 'Climate Change Report',
      sourceName: 'BBC',
      status: 'PUBLISHED',
      categoryName: 'Science',
      createdAt: '2026-04-26T08:00:00Z',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

const emptyPage: AdminArticleListPage = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe('ArticleListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAdminArticles).mockResolvedValue(mockPageData);
  });

  it('renders the page heading', () => {
    vi.mocked(getAdminArticles).mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('heading', { name: /articles/i })).toBeInTheDocument();
  });

  it('shows loading state before data arrives', () => {
    vi.mocked(getAdminArticles).mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getAllByText(/loading/i).length).toBeGreaterThan(0);
  });

  it('renders article rows after data loads', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('AI Revolution')).toBeInTheDocument();
      expect(screen.getByText('Climate Change Report')).toBeInTheDocument();
    });
  });

  it('displays source name for each article', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('TechCrunch')).toBeInTheDocument();
      expect(screen.getByText('BBC')).toBeInTheDocument();
    });
  });

  it('shows status badges', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Draft')).toBeInTheDocument();
      expect(screen.getByText('Published')).toBeInTheDocument();
    });
  });

  it('displays category names', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Technology')).toBeInTheDocument();
      expect(screen.getByText('Science')).toBeInTheDocument();
    });
  });

  it('shows empty state when no articles exist', async () => {
    vi.mocked(getAdminArticles).mockResolvedValue(emptyPage);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('No articles found')).toBeInTheDocument();
    });
  });

  it('shows error state on API failure', async () => {
    vi.mocked(getAdminArticles).mockRejectedValue(new Error('Network error'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
    });
  });

  it('renders a "New Article" link/button', () => {
    vi.mocked(getAdminArticles).mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('link', { name: /new article/i })).toBeInTheDocument();
  });

  it('renders status filter select', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByLabelText(/status/i)).toBeInTheDocument();
    });
  });

  it('calls API with status filter when changed', async () => {
    const user = userEvent.setup();
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('AI Revolution')).toBeInTheDocument();
    });

    const select = screen.getByLabelText(/status/i);
    await user.selectOptions(select, 'DRAFT');

    await waitFor(() => {
      expect(getAdminArticles).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'DRAFT' }),
      );
    });
  });

  it('shows pagination info', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/2 articles/i)).toBeInTheDocument();
    });
  });
});
