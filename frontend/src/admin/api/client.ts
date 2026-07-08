import { request, ApiError } from '../../api/client';
import { getApiBaseUrl } from '../../api/baseUrl';
import { adminAuthHeaders, notifyAdminUnauthorized } from './token';
import type {
  AdminArticleDetailResponse,
  AdminArticleListPage,
  CategoryResponse,
  GenerationStatusResponse,
  RegisterArticleRequest,
  RegisterArticleResponse,
} from './types';

export interface AdminArticleListParams {
  page?: number;
  size?: number;
  status?: string;
}

/**
 * Wraps the shared request() with the admin token header. On a 401 it clears the
 * stored token and notifies the shell so the token gate reappears.
 */
async function adminRequest<T>(path: string, init?: RequestInit): Promise<T> {
  try {
    return await request<T>(path, {
      ...init,
      headers: { ...adminAuthHeaders(), ...(init?.headers as Record<string, string> | undefined) },
    });
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      notifyAdminUnauthorized();
    }
    throw err;
  }
}

export function getAdminArticles(
  params: AdminArticleListParams = {},
): Promise<AdminArticleListPage> {
  const q = new URLSearchParams();
  if (params.page !== undefined) q.set('page', String(params.page));
  if (params.size !== undefined) q.set('size', String(params.size));
  if (params.status) q.set('status', params.status);
  const qs = q.toString();
  return adminRequest<AdminArticleListPage>(`/api/admin/articles${qs ? `?${qs}` : ''}`);
}

export function getAdminCategories(all: boolean = false): Promise<CategoryResponse[]> {
  const qs = all ? '?all=true' : '';
  return adminRequest<CategoryResponse[]>(`/api/admin/categories${qs}`);
}

export function createAdminCategory(
  body: { name: string; displayName: string; sortOrder: number; active: boolean }
): Promise<CategoryResponse> {
  return adminRequest<CategoryResponse>('/api/admin/categories', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function updateAdminCategory(
  id: string,
  body: { name: string; displayName: string; sortOrder: number; active: boolean }
): Promise<CategoryResponse> {
  return adminRequest<CategoryResponse>(`/api/admin/categories/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}

export function deleteAdminCategory(id: string): Promise<void> {
  return adminRequest<void>(`/api/admin/categories/${id}`, {
    method: 'DELETE',
  });
}

export function registerAdminArticle(
  body: RegisterArticleRequest,
): Promise<RegisterArticleResponse> {
  return adminRequest<RegisterArticleResponse>('/api/admin/articles', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function getAdminArticleDetail(articleId: string): Promise<AdminArticleDetailResponse> {
  return adminRequest<AdminArticleDetailResponse>(`/api/admin/articles/${articleId}`);
}

export function getGenerationStatus(articleId: string): Promise<GenerationStatusResponse> {
  return adminRequest<GenerationStatusResponse>(
    `/api/admin/articles/${articleId}/generation-status`,
  );
}

/** Shared handler for the raw-fetch retry endpoints (which return 204 No Content). */
async function adminMutate(path: string): Promise<void> {
  const base = getApiBaseUrl();
  const res = await fetch(`${base}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...adminAuthHeaders() },
  });
  if (!res.ok) {
    const body = await res
      .json()
      .catch(() => ({ error: 'Error', message: res.statusText }));
    if (res.status === 401) {
      notifyAdminUnauthorized();
    }
    throw new ApiError(res.status, body);
  }
}

export function retrySubJob(
  articleId: string,
  jobId: string,
  subJobId: string,
): Promise<void> {
  return adminMutate(
    `/api/admin/articles/${articleId}/generation-jobs/${jobId}/sub-jobs/${subJobId}/retry`,
  );
}

export function retryStep(
  articleId: string,
  jobId: string,
  subJobId: string,
  stepType: string,
): Promise<void> {
  return adminMutate(
    `/api/admin/articles/${articleId}/generation-jobs/${jobId}/sub-jobs/${subJobId}/steps/${stepType}/retry`,
  );
}

export function updateAdminArticleStatus(
  articleId: string,
  status: string,
): Promise<{ articleId: string; status: string }> {
  return adminRequest<{ articleId: string; status: string }>(
    `/api/admin/articles/${articleId}/status`,
    {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    },
  );
}
