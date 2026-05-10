import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CategoryPage from '../CategoryPage';
import { getAdminCategories, createAdminCategory, updateAdminCategory, deleteAdminCategory } from '../../api/client';
import type { CategoryResponse } from '../../api/types';

vi.mock('../../api/client', () => ({
  getAdminCategories: vi.fn(),
  createAdminCategory: vi.fn(),
  updateAdminCategory: vi.fn(),
  deleteAdminCategory: vi.fn(),
}));

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <CategoryPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const mockCategories: CategoryResponse[] = [
  { id: '1', name: 'Technology', slug: 'tech', sortOrder: 1, active: true },
  { id: '2', name: 'Science', slug: 'science', sortOrder: 2, active: false },
];

describe('CategoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAdminCategories).mockResolvedValue(mockCategories);
  });

  it('renders page heading and fetches all categories', async () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /categories/i })).toBeInTheDocument();
    await waitFor(() => {
      expect(getAdminCategories).toHaveBeenCalledWith(true);
    });
  });

  it('displays a table of categories', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Technology')).toBeInTheDocument();
      expect(screen.getByText('tech')).toBeInTheDocument();
      expect(screen.getByText('Science')).toBeInTheDocument();
      expect(screen.getByText('science')).toBeInTheDocument();
    });
  });

  it('opens create form when New Category is clicked', async () => {
    renderPage();
    await waitFor(() => screen.getByText('Technology'));
    
    fireEvent.click(screen.getByRole('button', { name: /new category/i }));
    
    expect(screen.getByRole('heading', { name: /create category/i })).toBeInTheDocument();
  });

  it('creates a category and refreshes list', async () => {
    vi.mocked(createAdminCategory).mockResolvedValue({
      id: '3', name: 'Business', slug: 'business', sortOrder: 3, active: true,
    });
    renderPage();
    await waitFor(() => screen.getByText('Technology'));
    
    fireEvent.click(screen.getByRole('button', { name: /new category/i }));
    
    fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: 'Business' } });
    fireEvent.change(screen.getByLabelText(/slug/i), { target: { value: 'business' } });
    fireEvent.change(screen.getByLabelText(/sort order/i), { target: { value: '3' } });
    
    const form = screen.getByLabelText(/display name/i).closest('form')!;
    fireEvent.submit(form);
    
    await waitFor(() => {
      expect(createAdminCategory).toHaveBeenCalledWith({
        name: 'business',
        displayName: 'Business',
        sortOrder: 3,
        active: true,
      });
      expect(getAdminCategories).toHaveBeenCalledTimes(2);
    });
  });

  it('opens edit form with populated data', async () => {
    renderPage();
    await waitFor(() => screen.getByText('Technology'));
    
    const editButtons = screen.getAllByRole('button', { name: /edit/i });
    fireEvent.click(editButtons[0]);
    
    expect(screen.getByRole('heading', { name: /edit category/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/display name/i)).toHaveValue('Technology');
  });

  it('updates a category', async () => {
    vi.mocked(updateAdminCategory).mockResolvedValue({
      id: '1', name: 'Tech Updated', slug: 'tech', sortOrder: 1, active: true,
    });
    renderPage();
    await waitFor(() => screen.getByText('Technology'));
    
    const editButtons = screen.getAllByRole('button', { name: /edit/i });
    fireEvent.click(editButtons[0]);
    
    fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: 'Tech Updated' } });
    
    const form = screen.getByLabelText(/display name/i).closest('form')!;
    fireEvent.submit(form);
    
    await waitFor(() => {
      expect(updateAdminCategory).toHaveBeenCalledWith('1', {
        name: 'tech',
        displayName: 'Tech Updated',
        sortOrder: 1,
        active: true,
      });
      expect(getAdminCategories).toHaveBeenCalledTimes(2);
    });
  });

  it('deletes a category', async () => {
    vi.mocked(deleteAdminCategory).mockResolvedValue();
    renderPage();
    await waitFor(() => screen.getByText('Technology'));
    
    const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
    
    // Override window.confirm in a way that JS DOM definitely respects
    const originalConfirm = window.confirm;
    window.confirm = () => true;
    
    fireEvent.click(deleteButtons[0]);
    
    await waitFor(() => {
      expect(deleteAdminCategory).toHaveBeenCalledWith('1');
      expect(getAdminCategories).toHaveBeenCalledTimes(2);
    });
    
    window.confirm = originalConfirm;
  });
});
