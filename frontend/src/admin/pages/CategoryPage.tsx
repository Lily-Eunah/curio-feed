import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';
import Button from '../components/Button';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import TextInput from '../components/TextInput';
import { getAdminCategories, createAdminCategory, updateAdminCategory, deleteAdminCategory } from '../api/client';
import type { CategoryResponse } from '../api/types';

export default function CategoryPage() {
  const queryClient = useQueryClient();
  const [isCreating, setIsCreating] = useState(false);
  const [editingCategory, setEditingCategory] = useState<CategoryResponse | null>(null);

  // Form state
  const [displayName, setDisplayName] = useState('');
  const [slug, setSlug] = useState('');
  const [sortOrder, setSortOrder] = useState('1');
  const [active, setActive] = useState(true);

  const { data: categories, isLoading, isError, refetch } = useQuery({
    queryKey: ['adminCategories', 'all'],
    queryFn: () => getAdminCategories(true),
  });

  type CategoryPayload = { name: string; displayName: string; sortOrder: number; active: boolean };

  const createMutation = useMutation({
    mutationFn: (payload: CategoryPayload) => createAdminCategory(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategories'] });
      resetForm();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string, payload: CategoryPayload }) => updateAdminCategory(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategories'] });
      resetForm();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteAdminCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategories'] });
    },
  });

  const handleNewClick = () => {
    setIsCreating(true);
    setEditingCategory(null);
    setDisplayName('');
    setSlug('');
    setSortOrder(String((categories?.length || 0) + 1));
    setActive(true);
  };

  const handleEditClick = (category: CategoryResponse) => {
    setIsCreating(false);
    setEditingCategory(category);
    setDisplayName(category.name); // displayName maps to name in response
    setSlug(category.slug);
    setSortOrder(String(category.sortOrder));
    setActive(category.active);
  };

  const resetForm = () => {
    setIsCreating(false);
    setEditingCategory(null);
    setDisplayName('');
    setSlug('');
    setSortOrder('1');
    setActive(true);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    const payload = {
      name: slug,
      displayName,
      sortOrder: parseInt(sortOrder, 10),
      active,
    };

    if (isCreating) {
      createMutation.mutate(payload);
    } else if (editingCategory) {
      updateMutation.mutate({ id: editingCategory.id, payload });
    }
  };

  const handleDelete = (category: CategoryResponse) => {
    if (window.confirm(`Are you sure you want to delete or deactivate category "${category.name}"?`)) {
      deleteMutation.mutate(category.id);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Categories" description="Manage article categories">
        {(!isCreating && !editingCategory) && (
          <Button variant="primary" onClick={handleNewClick}>New Category</Button>
        )}
      </PageHeader>

      {isLoading && <LoadingState message="Loading categories…" />}

      {isError && (
        <ErrorState
          title="Failed to load categories"
          message="Could not fetch the category list. Please try again."
          onRetry={() => refetch()}
        />
      )}

      {(isCreating || editingCategory) && (
        <Card>
          <h2 className="text-lg font-semibold mb-4">
            {isCreating ? 'Create Category' : 'Edit Category'}
          </h2>
          <form onSubmit={handleSave} className="space-y-4 max-w-xl">
            <TextInput
              label="Display Name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
              placeholder="e.g. Technology"
            />
            <TextInput
              label="Slug (Internal Name)"
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              required
              placeholder="e.g. tech"
              pattern="^[a-z0-9-]+$"
              title="Only lowercase letters, numbers, and hyphens"
            />
            <TextInput
              label="Sort Order"
              type="number"
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
              required
              min="0"
            />
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="active"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-600"
              />
              <label htmlFor="active" className="text-sm font-medium text-gray-700">
                Active (visible to users)
              </label>
            </div>
            
            <div className="flex space-x-3 pt-4">
              <Button type="submit" variant="primary" loading={createMutation.isPending || updateMutation.isPending}>
                Save
              </Button>
              <Button type="button" variant="secondary" onClick={resetForm}>
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      )}

      {!isLoading && !isError && categories && !isCreating && !editingCategory && (
        <Card noPadding>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Display Name
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Slug
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Sort Order
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th scope="col" className="relative px-6 py-3">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {categories.map((cat) => (
                  <tr key={cat.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {cat.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {cat.slug}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {cat.sortOrder}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset ${
                        cat.active ? 'bg-green-50 text-green-700 ring-green-600/20' : 'bg-gray-50 text-gray-600 ring-gray-500/10'
                      }`}>
                        {cat.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleEditClick(cat)}
                        className="text-indigo-600 hover:text-indigo-900 mr-4"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(cat)}
                        className="text-red-600 hover:text-red-900"
                        disabled={deleteMutation.isPending}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {categories.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-6 py-10 text-center text-sm text-gray-500">
                      No categories found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
