import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusBadge from '../components/StatusBadge';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import { getAdminArticles } from '../api/client';

// Must match the ArticleStatus enum. REVIEWING/HIDDEN/FAILED were listed here but do not
// exist server-side, so selecting them failed the request with a 400.
// The empty value is the working list: everything except archived.
const STATUS_OPTIONS = [
  { value: '', label: 'Active' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'ARCHIVED', label: 'Archived' },
];

const PAGE_SIZE = 20;

export default function ArticleListPage() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['adminArticles', page, statusFilter],
    queryFn: () =>
      getAdminArticles({
        page,
        size: PAGE_SIZE,
        status: statusFilter || undefined,
      }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Articles" description="Manage registered articles">
        <Link to="/admin/articles/new">
          <Button>New Article</Button>
        </Link>
      </PageHeader>

      {/* Filters */}
      <div className="flex items-center gap-4">
        <div>
          <label htmlFor="statusFilter" className="block text-xs font-medium text-gray-500 mb-1">
            Status
          </label>
          <select
            id="statusFilter"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {isLoading && <LoadingState />}

      {isError && (
        <ErrorState
          title="Failed to load articles"
          message="Could not fetch the article list. Please try again."
          onRetry={() => refetch()}
        />
      )}

      {data && data.content.length === 0 && (
        <EmptyState
          title="No articles found"
          message="There are no articles matching the current filter."
        >
          <Link to="/admin/articles/new">
            <Button>Create your first article</Button>
          </Link>
        </EmptyState>
      )}

      {data && data.content.length > 0 && (
        <Card className="p-0">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Title
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Source
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Category
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Levels
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Created
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {data.content.map((article) => (
                  <tr key={article.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 text-sm font-medium text-gray-900 max-w-xs truncate">
                      {article.originalTitle}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {article.sourceName}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {article.categoryName}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={article.status} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      {/* Status cannot show this: a failed generation and a ready article are both
                          DRAFT. Publishing below 3/3 makes the feed fall back to the source text. */}
                      <span
                        className={
                          article.populatedLevels >= 3
                            ? 'text-gray-500'
                            : 'font-medium text-red-600'
                        }
                        title={
                          article.populatedLevels >= 3
                            ? 'All difficulty levels generated'
                            : 'Incomplete — cannot be published until all three levels are generated'
                        }
                      >
                        {article.populatedLevels}/3
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                      {new Date(article.createdAt).toLocaleDateString()}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-right text-sm">
                      <Link
                        to={`/admin/articles/${article.id}`}
                        className="text-indigo-600 hover:text-indigo-900 font-medium"
                      >
                        View
                      </Link>
                      <span className="mx-2 text-gray-300">|</span>
                      <Link
                        to={`/admin/articles/${article.id}/status`}
                        className="text-indigo-600 hover:text-indigo-900 font-medium"
                      >
                        Status
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination footer */}
          <div className="flex items-center justify-between border-t border-gray-200 px-6 py-3">
            <p className="text-sm text-gray-700">
              {data.totalElements} article{data.totalElements !== 1 ? 's' : ''}
              {data.totalPages > 1 && (
                <span className="text-gray-400">
                  {' '}· Page {data.number + 1} of {data.totalPages}
                </span>
              )}
            </p>
            {data.totalPages > 1 && (
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  disabled={data.first}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  disabled={data.last}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            )}
          </div>
        </Card>
      )}
    </div>
  );
}
