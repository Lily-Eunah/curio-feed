import { describe, it, expect, vi, afterEach } from 'vitest';
import { fetchFeedArticles, fetchArticleDetail, submitQuizAttempt, ApiError } from '../client';

const BASE = 'http://localhost:8080';

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

const emptyPage = { data: [], nextCursor: null, hasNext: false };

// ── fetchFeedArticles ────────────────────────────────────────────────────────

describe('fetchFeedArticles', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('includes VITE_API_BASE_URL in request URL', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY' });
    expect((mock.mock.calls[0][0] as string).startsWith(BASE)).toBe(true);
  });

  it('sends level query param', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'MEDIUM' });
    expect(mock.mock.calls[0][0] as string).toContain('level=MEDIUM');
  });

  it('sends category param when category is a specific value', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY', category: 'Tech' });
    expect(mock.mock.calls[0][0] as string).toContain('category=Tech');
  });

  it('omits category param when category is "All"', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY', category: 'All' });
    expect(mock.mock.calls[0][0] as string).not.toContain('category=');
  });

  it('omits category param when category is "ALL"', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY', category: 'ALL' });
    expect(mock.mock.calls[0][0] as string).not.toContain('category=');
  });

  it('omits category param when category is not provided', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY' });
    expect(mock.mock.calls[0][0] as string).not.toContain('category=');
  });

  it('sends cursor param when provided', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY', cursor: 'tok_abc' });
    expect(mock.mock.calls[0][0] as string).toContain('cursor=');
  });

  it('omits cursor param when not provided', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY' });
    expect(mock.mock.calls[0][0] as string).not.toContain('cursor=');
  });

  it('sends size param when provided', async () => {
    const mock = mockOk(emptyPage);
    vi.stubGlobal('fetch', mock);
    await fetchFeedArticles({ level: 'EASY', size: 5 });
    expect(mock.mock.calls[0][0] as string).toContain('size=5');
  });

  it('returns typed CursorPageResponse', async () => {
    const payload = {
      data: [{
        id: 'u1', title: 'Title', categoryName: 'Tech', sourceName: 'Src',
        thumbnailUrl: '', publishedAt: '2026-01-01T00:00:00Z',
        estimatedReadingTime: 3, excerpt: 'E',
      }],
      nextCursor: 'next_tok',
      hasNext: true,
    };
    vi.stubGlobal('fetch', mockOk(payload));
    const result = await fetchFeedArticles({ level: 'EASY' });
    expect(result.data).toHaveLength(1);
    expect(result.data[0].title).toBe('Title');
    expect(result.hasNext).toBe(true);
    expect(result.nextCursor).toBe('next_tok');
  });

  it('throws ApiError on non-ok response', async () => {
    vi.stubGlobal('fetch', mockErr(404, { error: 'Not Found', message: 'not found' }));
    await expect(fetchFeedArticles({ level: 'EASY' })).rejects.toBeInstanceOf(ApiError);
  });

  it('ApiError carries HTTP status and message', async () => {
    vi.stubGlobal('fetch', mockErr(400, { error: 'Bad Request', message: 'invalid level' }));
    const err = await fetchFeedArticles({ level: 'EASY' }).catch(e => e);
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).status).toBe(400);
    expect((err as ApiError).message).toBe('invalid level');
  });
});

// ── fetchArticleDetail ───────────────────────────────────────────────────────

describe('fetchArticleDetail', () => {
  afterEach(() => vi.unstubAllGlobals());

  const detail = {
    id: 'u1', title: 'T', originalTitle: 'OT', sourceName: 'S', sourceUrl: 'http://s',
    thumbnailUrl: '', publishedAt: '2026-01-01T00:00:00Z', categoryName: 'Tech',
    availableLevels: ['EASY'],
    content: { id: 'c1', level: 'EASY', content: 'body', audioUrl: null, vocabularies: [], quizzes: [] },
  };

  it('calls /api/articles/{id}?level={level}', async () => {
    const mock = mockOk(detail);
    vi.stubGlobal('fetch', mock);
    await fetchArticleDetail('abc-123', 'HARD');
    expect(mock.mock.calls[0][0] as string).toMatch(/\/api\/articles\/abc-123\?level=HARD/);
  });

  it('includes base URL in request', async () => {
    const mock = mockOk(detail);
    vi.stubGlobal('fetch', mock);
    await fetchArticleDetail('abc-123', 'EASY');
    expect((mock.mock.calls[0][0] as string).startsWith(BASE)).toBe(true);
  });

  it('returns ArticleDetailDto with content', async () => {
    vi.stubGlobal('fetch', mockOk(detail));
    const result = await fetchArticleDetail('u1', 'EASY');
    expect(result.id).toBe('u1');
    expect(result.content.id).toBe('c1');
    expect(result.content.level).toBe('EASY');
  });

  it('throws ApiError on 404', async () => {
    vi.stubGlobal('fetch', mockErr(404, { error: 'Not Found', message: 'Article not found' }));
    await expect(fetchArticleDetail('bad', 'EASY')).rejects.toBeInstanceOf(ApiError);
  });

  it('ApiError carries 404 status', async () => {
    vi.stubGlobal('fetch', mockErr(404, { error: 'Not Found', message: 'Article not found' }));
    const err = await fetchArticleDetail('bad', 'EASY').catch(e => e);
    expect((err as ApiError).status).toBe(404);
  });
});

// ── submitQuizAttempt ────────────────────────────────────────────────────────

describe('submitQuizAttempt', () => {
  afterEach(() => vi.unstubAllGlobals());

  const okResponse = { isCorrect: true, correctAnswer: 'A', explanation: 'Well done' };

  it('POSTs to correct quiz attempt URL', async () => {
    const mock = mockOk(okResponse);
    vi.stubGlobal('fetch', mock);
    await submitQuizAttempt('art1', 'con1', 'qz1', { choiceId: 'A' });
    const url = mock.mock.calls[0][0] as string;
    expect(url).toContain('/api/articles/art1/contents/con1/quizzes/qz1/attempts');
    expect((mock.mock.calls[0][1] as RequestInit).method).toBe('POST');
  });

  it('serializes choiceId in JSON body', async () => {
    const mock = mockOk(okResponse);
    vi.stubGlobal('fetch', mock);
    await submitQuizAttempt('art1', 'con1', 'qz1', { choiceId: 'B' });
    const body = JSON.parse((mock.mock.calls[0][1] as RequestInit).body as string);
    expect(body.choiceId).toBe('B');
  });

  it('returns QuizAttemptResponse with isCorrect and explanation', async () => {
    vi.stubGlobal('fetch', mockOk(okResponse));
    const result = await submitQuizAttempt('art1', 'con1', 'qz1', { choiceId: 'A' });
    expect(result.isCorrect).toBe(true);
    expect(result.explanation).toBe('Well done');
  });

  it('correctAnswer can be a string for MCQ', async () => {
    vi.stubGlobal('fetch', mockOk({ isCorrect: false, correctAnswer: 'B', explanation: 'nope' }));
    const result = await submitQuizAttempt('art1', 'con1', 'qz1', { choiceId: 'A' });
    expect(result.correctAnswer).toBe('B');
  });

  it('throws ApiError with 400 on missing choiceId', async () => {
    vi.stubGlobal('fetch', mockErr(400, { error: 'Bad Request', message: 'MULTIPLE_CHOICE requires a valid choiceId' }));
    const err = await submitQuizAttempt('art1', 'con1', 'qz1', {}).catch(e => e);
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).status).toBe(400);
    expect((err as ApiError).message).toBe('MULTIPLE_CHOICE requires a valid choiceId');
  });

  it('throws ApiError with 404 when quiz not found', async () => {
    vi.stubGlobal('fetch', mockErr(404, { error: 'Not Found', message: 'Quiz not found' }));
    await expect(submitQuizAttempt('art1', 'con1', 'bad', { choiceId: 'A' })).rejects.toBeInstanceOf(ApiError);
  });
});
