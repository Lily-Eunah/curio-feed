import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';
import TextInput from '../components/TextInput';
import Textarea from '../components/Textarea';
import Select from '../components/Select';
import DateTimeInput from '../components/DateTimeInput';
import Button from '../components/Button';
import { getAdminCategories, registerAdminArticle } from '../api/client';

export default function ArticleCreatePage() {
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { data: categories = [], isLoading: isLoadingCategories } = useQuery({
    queryKey: ['adminCategories'],
    queryFn: () => getAdminCategories(),
  });

  const mutation = useMutation({ mutationFn: registerAdminArticle });

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitError(null);
    const fd = new FormData(e.currentTarget);

    // datetime-local returns 'YYYY-MM-DDThh:mm'; append ':00Z' to parse as UTC
    const rawDate = fd.get('originalPublishedAt') as string;
    const publishedAtStr = new Date(rawDate + ':00Z').toISOString();

    try {
      const result = await mutation.mutateAsync({
        originalTitle: fd.get('originalTitle') as string,
        sourceName: fd.get('sourceName') as string,
        sourceUrl: fd.get('sourceUrl') as string,
        originalContent: fd.get('originalContent') as string,
        originalPublishedAt: publishedAtStr,
        categoryId: fd.get('categoryId') as string,
      });
      navigate(`/admin/articles/${result.articleId}/status`);
    } catch (err) {
      setSubmitError((err as Error).message || 'Failed to create article');
    }
  };

  const categoryOptions = categories.map((c) => ({
    value: c.id,
    label: c.name,
  }));

  return (
    <div className="space-y-6 max-w-4xl">
      <PageHeader title="New Article" />

      {submitError && (
        <div className="rounded-md bg-red-50 p-4 border border-red-200">
          <p className="text-sm text-red-700">{submitError}</p>
        </div>
      )}

      <Card>
        <form onSubmit={handleSubmit} className="space-y-6">
          <TextInput
            label="Original Title"
            name="originalTitle"
            required
            placeholder="e.g. AI is changing the world"
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <TextInput
              label="Source Name"
              name="sourceName"
              required
              placeholder="e.g. BBC News"
            />
            <TextInput
              label="Source URL"
              name="sourceUrl"
              type="url"
              required
              placeholder="https://..."
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <DateTimeInput
              label="Original Published At"
              name="originalPublishedAt"
              required
            />
            <Select
              label="Category"
              name="categoryId"
              required
              options={
                isLoadingCategories
                  ? [{ value: '', label: 'Loading categories...' }]
                  : [
                      { value: '', label: 'Select a category' },
                      ...categoryOptions,
                    ]
              }
              disabled={isLoadingCategories}
            />
          </div>

          <Textarea
            label="Original Content"
            name="originalContent"
            required
            rows={10}
            placeholder="Paste the full article content here..."
          />

          <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate(-1)}
              disabled={mutation.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" loading={mutation.isPending}>
              Create Article
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
