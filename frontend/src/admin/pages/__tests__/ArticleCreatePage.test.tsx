import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ArticleCreatePage from '../ArticleCreatePage';
import { getAdminCategories, registerAdminArticle } from '../../api/client';

vi.mock('../../api/client', () => ({
  getAdminCategories: vi.fn(),
  registerAdminArticle: vi.fn(),
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
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ArticleCreatePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('ArticleCreatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAdminCategories).mockResolvedValue([
      { id: 'cat-1', name: 'Tech', slug: 'tech', sortOrder: 1, active: true },
      { id: 'cat-2', name: 'Science', slug: 'science', sortOrder: 2, active: true },
    ]);
  });

  it('renders required form fields', async () => {
    renderPage();
    expect(screen.getByLabelText(/original title/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/source name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/source url/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/original content/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/original published at/i)).toBeInTheDocument();
    
    // Category select should render with fetched options
    await waitFor(() => {
      expect(screen.getByLabelText(/category/i)).toBeInTheDocument();
      expect(screen.getAllByRole('option').length).toBeGreaterThan(1);
    });
  });

  it('shows error states if submitted empty', async () => {
    const user = userEvent.setup();
    renderPage();
    
    // wait for categories to load
    await waitFor(() => {
      expect(screen.getAllByRole('option').length).toBeGreaterThan(1);
    });

    const submitBtn = screen.getAllByRole('button', { name: /create article/i })[0];
    await user.click(submitBtn);

    // HTML5 validation requires invalid elements
    // The inputs should be required
    expect(screen.getByLabelText(/original title/i)).toBeRequired();
    expect(screen.getByLabelText(/source name/i)).toBeRequired();
    expect(screen.getByLabelText(/source url/i)).toBeRequired();
    expect(screen.getByLabelText(/original content/i)).toBeRequired();
    expect(screen.getByLabelText(/original published at/i)).toBeRequired();
    expect(screen.getByLabelText(/category/i)).toBeRequired();
  });

  it('submits successfully and navigates to the new article', async () => {
    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getAllByRole('option').length).toBeGreaterThan(1);
    });

    vi.mocked(registerAdminArticle).mockResolvedValueOnce({
      articleId: 'a123',
      jobId: 'j456',
      status: 'PENDING',
    });

    await user.type(screen.getByLabelText(/original title/i), 'Test Title');
    await user.type(screen.getByLabelText(/source name/i), 'BBC');
    await user.type(screen.getByLabelText(/source url/i), 'https://bbc.com/test');
    await user.type(screen.getByLabelText(/original content/i), 'Test content');
    // Using simple text input for datetime for now since test-userEvent doesn't easily trigger native date pickers.
    // Note: The UI component might need to be a text input or we interact with the native datetime-local strictly.
    const dateInput = screen.getByLabelText(/original published at/i);
    // userEvent type works with datetime-local if we format it YYYY-MM-DDThh:mm
    await user.type(dateInput, '2026-04-26T14:30');
    
    await user.selectOptions(screen.getByLabelText(/category/i), 'cat-1');

    const submitBtn = screen.getAllByRole('button', { name: /create article/i })[0];
    await user.click(submitBtn);

    await waitFor(() => {
      expect(registerAdminArticle).toHaveBeenCalledWith(
        {
          originalTitle: 'Test Title',
          sourceName: 'BBC',
          sourceUrl: 'https://bbc.com/test',
          originalContent: 'Test content',
          originalPublishedAt: '2026-04-26T14:30:00.000Z',
          categoryId: 'cat-1',
        },
        expect.anything(), // React Query v5 passes mutation context as second arg
      );
    });

    expect(mockNavigate).toHaveBeenCalledWith('/admin/articles/a123/status');
  });

  it('shows error alert on API failure', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getAllByRole('option').length).toBeGreaterThan(1);
    });

    vi.mocked(registerAdminArticle).mockRejectedValueOnce(
      new Error('Failed to register article')
    );

    // Use fireEvent.change to directly set input values, bypassing jsdom's
    // character-by-character datetime-local validation that blocks form submission
    fireEvent.change(screen.getByLabelText(/original title/i), { target: { value: 'Test Title' } });
    fireEvent.change(screen.getByLabelText(/source name/i), { target: { value: 'BBC' } });
    fireEvent.change(screen.getByLabelText(/source url/i), { target: { value: 'https://bbc.com' } });
    fireEvent.change(screen.getByLabelText(/original content/i), { target: { value: 'content' } });
    fireEvent.change(screen.getByLabelText(/original published at/i), { target: { value: '2026-04-26T14:30' } });
    fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'cat-1' } });

    const form = screen.getByLabelText(/original title/i).closest('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText(/failed to register article/i)).toBeInTheDocument();
    });
  });
});
