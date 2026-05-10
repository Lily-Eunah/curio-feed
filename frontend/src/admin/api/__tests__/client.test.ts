import { describe, it, expect, vi, afterEach } from 'vitest';
import { getAdminArticles, getAdminCategories, registerAdminArticle, getGenerationStatus, retrySubJob, updateAdminArticleStatus, createAdminCategory, updateAdminCategory, deleteAdminCategory } from '../client';
import { ApiError } from '../../../api/client';
import type { RegisterArticleRequest } from '../types';

function mockOk(data: unknown) {
  return vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(data),
  });
}

function mockErr(status: number, body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: false,
    status,
    statusText: String(status),
    json: () => Promise.resolve(body),
  });
}

describe('Admin API Client', () => {
  afterEach(() => vi.unstubAllGlobals());

  describe('getAdminCategories', () => {
    it('fetches from /api/admin/categories', async () => {
      const mock = mockOk([]);
      vi.stubGlobal('fetch', mock);
      
      const result = await getAdminCategories();
      
      expect(mock.mock.calls[0][0] as string).toMatch(/\/api\/admin\/categories$/);
      expect(result).toEqual([]);
    });

    it('fetches with ?all=true', async () => {
      const mock = mockOk([]);
      vi.stubGlobal('fetch', mock);
      
      await getAdminCategories(true);
      
      expect(mock.mock.calls[0][0] as string).toMatch(/\/api\/admin\/categories\?all=true$/);
    });

    it('returns typed CategoryResponse array', async () => {
      const categories = [
        { id: '1', name: 'Tech', slug: 'tech', sortOrder: 1, active: true },
        { id: '2', name: 'Science', slug: 'science', sortOrder: 2, active: false }
      ];
      vi.stubGlobal('fetch', mockOk(categories));
      
      const result = await getAdminCategories();
      expect(result).toHaveLength(2);
      expect(result[0].name).toBe('Tech');
    });
  });

  describe('registerAdminArticle', () => {
    const payload: RegisterArticleRequest = {
      originalTitle: 'Test',
      sourceName: 'Source',
      sourceUrl: 'https://test.com',
      originalContent: 'Content',
      originalPublishedAt: '2026-04-27T00:00:00Z',
      categoryId: 'cat-123'
    };

    it('posts to /api/admin/articles with correct body', async () => {
      const mock = mockOk({ articleId: 'a1', jobId: 'j1', status: 'PENDING' });
      vi.stubGlobal('fetch', mock);
      
      await registerAdminArticle(payload);
      
      expect(mock.mock.calls[0][0] as string).toMatch(/\/api\/admin\/articles$/);
      const reqInit = mock.mock.calls[0][1] as RequestInit;
      expect(reqInit.method).toBe('POST');
      expect(JSON.parse(reqInit.body as string)).toEqual(payload);
    });

    it('returns RegisterArticleResponse on success', async () => {
      const mockResponse = { articleId: 'a1', jobId: 'j1', status: 'PENDING' };
      vi.stubGlobal('fetch', mockOk(mockResponse));
      
      const result = await registerAdminArticle(payload);
      expect(result).toEqual(mockResponse);
    });

    it('throws ApiError on conflict (409)', async () => {
      vi.stubGlobal('fetch', mockErr(409, { error: 'Conflict', message: 'Article already exists' }));

      await expect(registerAdminArticle(payload)).rejects.toBeInstanceOf(ApiError);
    });
  });

  describe('getGenerationStatus', () => {
    it('fetches from /api/admin/articles/:id/generation-status', async () => {
      const mock = mockOk({ articleId: 'a1', articleStatus: 'DRAFT', job: null });
      vi.stubGlobal('fetch', mock);

      await getGenerationStatus('a1');

      expect(mock.mock.calls[0][0] as string).toMatch(
        /\/api\/admin\/articles\/a1\/generation-status$/,
      );
    });

    it('returns GenerationStatusResponse with job and subJobs', async () => {
      const response = {
        articleId: 'a1',
        articleStatus: 'REVIEWING',
        job: {
          jobId: 'j1',
          subJobs: [{ subJobId: 'sub-1', level: 'EASY', status: 'COMPLETED', retryCount: 0 }],
        },
      };
      vi.stubGlobal('fetch', mockOk(response));

      const result = await getGenerationStatus('a1');

      expect(result.articleStatus).toBe('REVIEWING');
      expect(result.job?.subJobs[0].level).toBe('EASY');
    });

    it('returns null job when no generation job exists yet', async () => {
      vi.stubGlobal('fetch', mockOk({ articleId: 'a1', articleStatus: 'DRAFT', job: null }));

      const result = await getGenerationStatus('a1');

      expect(result.job).toBeNull();
    });

    it('throws ApiError on 404', async () => {
      vi.stubGlobal('fetch', mockErr(404, { error: 'Not Found', message: 'Article not found' }));

      await expect(getGenerationStatus('no-such-id')).rejects.toBeInstanceOf(ApiError);
    });
  });

  describe('retrySubJob', () => {
    it('POSTs to the correct retry URL', async () => {
      const mock = vi.fn().mockResolvedValue({ ok: true, status: 204 });
      vi.stubGlobal('fetch', mock);

      await retrySubJob('art-1', 'job-1', 'sub-2');

      const url = mock.mock.calls[0][0] as string;
      expect(url).toMatch(
        /\/api\/admin\/articles\/art-1\/generation-jobs\/job-1\/sub-jobs\/sub-2\/retry$/,
      );
      expect((mock.mock.calls[0][1] as RequestInit).method).toBe('POST');
    });

    it('resolves void on 204 success', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 204 }));

      await expect(retrySubJob('art-1', 'job-1', 'sub-2')).resolves.toBeUndefined();
    });

    it('throws ApiError on 400', async () => {
      vi.stubGlobal(
        'fetch',
        mockErr(400, { error: 'Bad Request', message: 'SubJob is not in FAILED state' }),
      );

      await expect(retrySubJob('art-1', 'job-1', 'sub-2')).rejects.toBeInstanceOf(ApiError);
    });
  });

  describe('getAdminArticles', () => {
    const mockPage = {
      content: [
        {
          id: 'a1',
          originalTitle: 'Test Article',
          sourceName: 'BBC',
          status: 'DRAFT',
          categoryName: 'Tech',
          createdAt: '2026-04-27T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
      first: true,
      last: true,
    };

    it('fetches from /api/admin/articles with no params', async () => {
      const mock = mockOk(mockPage);
      vi.stubGlobal('fetch', mock);

      const result = await getAdminArticles();

      expect(mock.mock.calls[0][0] as string).toMatch(/\/api\/admin\/articles$/);
      expect(result.content).toHaveLength(1);
      expect(result.totalElements).toBe(1);
    });

    it('appends status query param when provided', async () => {
      const mock = mockOk(mockPage);
      vi.stubGlobal('fetch', mock);

      await getAdminArticles({ status: 'DRAFT' });

      const url = mock.mock.calls[0][0] as string;
      expect(url).toContain('status=DRAFT');
    });

    it('appends page and size params', async () => {
      const mock = mockOk(mockPage);
      vi.stubGlobal('fetch', mock);

      await getAdminArticles({ page: 1, size: 10 });

      const url = mock.mock.calls[0][0] as string;
      expect(url).toContain('page=1');
      expect(url).toContain('size=10');
    });

    it('throws ApiError on failure', async () => {
      vi.stubGlobal('fetch', mockErr(500, { error: 'Server Error', message: 'Internal error' }));

      await expect(getAdminArticles()).rejects.toBeInstanceOf(ApiError);
    });
  });

  describe('updateAdminArticleStatus', () => {
    it('PATCHes to /api/admin/articles/:id/status', async () => {
      const mock = mockOk({ articleId: 'a1', status: 'PUBLISHED' });
      vi.stubGlobal('fetch', mock);

      await updateAdminArticleStatus('a1', 'PUBLISHED');

      const url = mock.mock.calls[0][0] as string;
      expect(url).toMatch(/\/api\/admin\/articles\/a1\/status$/);
      expect((mock.mock.calls[0][1] as RequestInit).method).toBe('PATCH');
    });

    it('sends status in the request body', async () => {
      const mock = mockOk({ articleId: 'a1', status: 'PUBLISHED' });
      vi.stubGlobal('fetch', mock);

      await updateAdminArticleStatus('a1', 'PUBLISHED');

      const body = JSON.parse((mock.mock.calls[0][1] as RequestInit).body as string);
      expect(body).toEqual({ status: 'PUBLISHED' });
    });

    it('returns updated articleId and status', async () => {
      vi.stubGlobal('fetch', mockOk({ articleId: 'a1', status: 'HIDDEN' }));

      const result = await updateAdminArticleStatus('a1', 'HIDDEN');
      expect(result).toEqual({ articleId: 'a1', status: 'HIDDEN' });
    });

    it('throws ApiError on 400 invalid transition', async () => {
      vi.stubGlobal('fetch', mockErr(400, { error: 'Bad Request', message: 'Invalid status transition' }));

      await expect(updateAdminArticleStatus('a1', 'HIDDEN')).rejects.toBeInstanceOf(ApiError);
    });
  });

  describe('createAdminCategory', () => {
    it('POSTs to /api/admin/categories', async () => {
      const mock = mockOk({ id: '1', name: 'Tech', slug: 'tech', sortOrder: 1, active: true });
      vi.stubGlobal('fetch', mock);

      const payload = { name: 'tech', displayName: 'Tech', sortOrder: 1, active: true };
      const result = await createAdminCategory(payload);

      const url = mock.mock.calls[0][0] as string;
      expect(url).toMatch(/\/api\/admin\/categories$/);
      expect((mock.mock.calls[0][1] as RequestInit).method).toBe('POST');
      expect(JSON.parse((mock.mock.calls[0][1] as RequestInit).body as string)).toEqual(payload);
      expect(result).toEqual({ id: '1', name: 'Tech', slug: 'tech', sortOrder: 1, active: true });
    });
  });

  describe('updateAdminCategory', () => {
    it('PATCHes to /api/admin/categories/:id', async () => {
      const mock = mockOk({ id: '1', name: 'Tech', slug: 'tech', sortOrder: 1, active: true });
      vi.stubGlobal('fetch', mock);

      const payload = { name: 'tech', displayName: 'Tech', sortOrder: 1, active: true };
      const result = await updateAdminCategory('1', payload);

      const url = mock.mock.calls[0][0] as string;
      expect(url).toMatch(/\/api\/admin\/categories\/1$/);
      expect((mock.mock.calls[0][1] as RequestInit).method).toBe('PATCH');
      expect(JSON.parse((mock.mock.calls[0][1] as RequestInit).body as string)).toEqual(payload);
      expect(result).toEqual({ id: '1', name: 'Tech', slug: 'tech', sortOrder: 1, active: true });
    });
  });

  describe('deleteAdminCategory', () => {
    it('DELETEs to /api/admin/categories/:id', async () => {
      const mock = mockOk({});
      vi.stubGlobal('fetch', mock);

      await deleteAdminCategory('1');

      const url = mock.mock.calls[0][0] as string;
      expect(url).toMatch(/\/api\/admin\/categories\/1$/);
      expect((mock.mock.calls[0][1] as RequestInit).method).toBe('DELETE');
    });
  });
});
