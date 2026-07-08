import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminRoutes from '../AdminRoutes';
import { setAdminToken, clearAdminToken } from '../api/token';

beforeEach(() => setAdminToken('test-token'));
afterEach(() => clearAdminToken());

function renderAtPath(path: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/admin/*" element={<AdminRoutes />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('Admin Routes', () => {
  it('renders Dashboard page heading at /admin/dashboard', () => {
    const { container } = renderAtPath('/admin/dashboard');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Dashboard');
  });

  it('renders Articles page heading at /admin/articles', () => {
    const { container } = renderAtPath('/admin/articles');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Articles');
  });

  it('renders New Article page heading at /admin/articles/new', () => {
    const { container } = renderAtPath('/admin/articles/new');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('New Article');
  });

  it('renders Generation Status page heading at /admin/articles/:id/status', () => {
    const { container } = renderAtPath('/admin/articles/some-uuid/status');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Generation Status');
  });

  it('renders Article Detail page heading at /admin/articles/:id', () => {
    const { container } = renderAtPath('/admin/articles/some-uuid');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Article some-uuid');
  });

  it('renders Jobs page heading at /admin/jobs', () => {
    const { container } = renderAtPath('/admin/jobs');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Jobs');
  });

  it('renders Job Detail page heading at /admin/jobs/:jobId', () => {
    const { container } = renderAtPath('/admin/jobs/some-job-id');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Job Detail');
  });

  it('renders Categories page heading at /admin/categories', () => {
    const { container } = renderAtPath('/admin/categories');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Categories');
  });

  it('redirects /admin to /admin/dashboard', () => {
    const { container } = renderAtPath('/admin');
    const h1 = container.querySelector('h1');
    expect(h1?.textContent).toBe('Dashboard');
  });

  it('preserves sidebar navigation on every page', () => {
    const { container } = renderAtPath('/admin/articles');
    const nav = container.querySelector('nav');
    expect(nav).not.toBeNull();
    const links = nav!.querySelectorAll('a');
    expect(links).toHaveLength(4);
  });
});
