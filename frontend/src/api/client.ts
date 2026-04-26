import type {
  ArticleDetailDto,
  ApiErrorBody,
  CursorPageResponse,
  DifficultyLevel,
  FeedArticleDto,
  QuizAttemptRequest,
  QuizAttemptResponse,
} from './types';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: ApiErrorBody,
  ) {
    super(body.message);
    this.name = 'ApiError';
  }
}

function baseUrl(): string {
  return (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${baseUrl()}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!res.ok) {
    const body: ApiErrorBody = await res
      .json()
      .catch(() => ({ error: 'Error', message: res.statusText }));
    throw new ApiError(res.status, body);
  }
  return res.json() as Promise<T>;
}

export interface FeedParams {
  level: DifficultyLevel;
  category?: string;
  cursor?: string;
  size?: number;
}

export function fetchFeedArticles(
  params: FeedParams,
): Promise<CursorPageResponse<FeedArticleDto>> {
  const q = new URLSearchParams();
  q.set('level', params.level);
  if (params.category && params.category !== 'All' && params.category !== 'ALL') {
    q.set('category', params.category);
  }
  if (params.cursor !== undefined) q.set('cursor', params.cursor);
  if (params.size !== undefined) q.set('size', String(params.size));
  return request<CursorPageResponse<FeedArticleDto>>(`/api/articles?${q.toString()}`);
}

export function fetchArticleDetail(
  id: string,
  level: DifficultyLevel,
): Promise<ArticleDetailDto> {
  return request<ArticleDetailDto>(`/api/articles/${id}?level=${level}`);
}

export function submitQuizAttempt(
  articleId: string,
  contentId: string,
  quizId: string,
  body: QuizAttemptRequest,
): Promise<QuizAttemptResponse> {
  return request<QuizAttemptResponse>(
    `/api/articles/${articleId}/contents/${contentId}/quizzes/${quizId}/attempts`,
    { method: 'POST', body: JSON.stringify(body) },
  );
}
