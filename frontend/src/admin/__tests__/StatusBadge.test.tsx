import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatusBadge from '../components/StatusBadge';

describe('StatusBadge', () => {
  it('renders PENDING with gray styling', () => {
    render(<StatusBadge status="PENDING" />);
    const badge = screen.getByText('Pending');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/gray/);
  });

  it('renders PROCESSING with blue styling', () => {
    render(<StatusBadge status="PROCESSING" />);
    const badge = screen.getByText('Processing');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/blue/);
  });

  it('renders RUNNING with blue styling', () => {
    render(<StatusBadge status="RUNNING" />);
    const badge = screen.getByText('Running');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/blue/);
  });

  it('renders COMPLETED with green styling', () => {
    render(<StatusBadge status="COMPLETED" />);
    const badge = screen.getByText('Completed');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/green/);
  });

  it('renders FAILED with red styling', () => {
    render(<StatusBadge status="FAILED" />);
    const badge = screen.getByText('Failed');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/red/);
  });

  it('renders DRAFT with gray styling', () => {
    render(<StatusBadge status="DRAFT" />);
    const badge = screen.getByText('Draft');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/gray/);
  });

  it('renders PUBLISHED with green styling', () => {
    render(<StatusBadge status="PUBLISHED" />);
    const badge = screen.getByText('Published');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/green/);
  });

  it('renders ARCHIVED with gray styling', () => {
    render(<StatusBadge status="ARCHIVED" />);
    const badge = screen.getByText('Archived');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toMatch(/gray/);
  });

  it('falls back to raw status string for unknown status', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    render(<StatusBadge status={'UNKNOWN_STATUS' as any} />);
    expect(screen.getByText('UNKNOWN_STATUS')).toBeInTheDocument();
  });
});
