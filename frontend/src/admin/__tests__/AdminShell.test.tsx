import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AdminShell from '../layout/AdminShell';
import { setAdminToken, clearAdminToken } from '../api/token';

beforeEach(() => setAdminToken('test-token'));
afterEach(() => clearAdminToken());

function renderShell() {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <AdminShell />
    </MemoryRouter>,
  );
}

describe('AdminShell', () => {
  it('renders the CurioFeed Admin brand text', () => {
    const { getByText } = renderShell();
    expect(getByText('CurioFeed Admin')).toBeInTheDocument();
  });

  it('renders four sidebar navigation links inside <nav>', () => {
    const { container } = renderShell();
    const nav = container.querySelector('nav');
    expect(nav).not.toBeNull();
    const links = nav!.querySelectorAll('a');
    expect(links).toHaveLength(4);
  });

  it('sidebar links point to correct paths', () => {
    const { container } = renderShell();
    const links = container.querySelectorAll('nav a');
    const hrefs = Array.from(links).map((l) => l.getAttribute('href'));
    expect(hrefs).toEqual([
      '/admin/dashboard',
      '/admin/articles',
      '/admin/jobs',
      '/admin/categories',
    ]);
  });

  it('sidebar links contain correct labels', () => {
    const { container } = renderShell();
    const nav = container.querySelector('nav')!;
    expect(nav.textContent).toContain('Dashboard');
    expect(nav.textContent).toContain('Articles');
    expect(nav.textContent).toContain('Jobs');
    expect(nav.textContent).toContain('Categories');
  });
});
