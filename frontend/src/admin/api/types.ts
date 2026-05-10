export type JobStatus = 'PENDING' | 'PROCESSING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
export type ArticleStatus = 'DRAFT' | 'REVIEWING' | 'PUBLISHED' | 'HIDDEN' | 'READY' | 'STALE';
export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';
export type GenerationStepType = 'CONTENT' | 'VOCABULARY' | 'QUIZ';

export interface StepJobInfo {
  stepJobId: string;
  stepType: GenerationStepType;
  status: JobStatus;
  attemptCount: number;
  startedAt?: string | null;
  completedAt?: string | null;
  validationStatus?: string | null;
  validationErrors?: string | null;
  errorMessage?: string | null;
}

export interface SubJobInfo {
  subJobId: string;
  level: DifficultyLevel;
  status: JobStatus;
  retryCount: number;
  lastHeartbeatAt?: string | null;
  steps: StepJobInfo[];
}

export interface JobInfo {
  jobId: string;
  subJobs: SubJobInfo[];
}

export interface GenerationStatusResponse {
  articleId: string;
  articleStatus: ArticleStatus;
  job: JobInfo | null;
}

export interface CategoryResponse {
  id: string;
  name: string;
  slug: string;
  sortOrder: number;
  active: boolean;
}

export interface RegisterArticleRequest {
  originalTitle: string;
  sourceName: string;
  sourceUrl: string;
  originalContent: string;
  originalPublishedAt: string;
  categoryId: string;
}

export interface RegisterArticleResponse {
  articleId: string;
  jobId: string;
  status: string;
}

export interface AdminArticleListItem {
  id: string;
  originalTitle: string;
  sourceName: string;
  status: ArticleStatus;
  categoryName: string;
  createdAt: string;
}

export interface AdminArticleListPage {
  content: AdminArticleListItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface AdminVocabInfo {
  id: string;
  word: string;
  definition: string;
  exampleSentence: string;
}

export interface AdminQuizInfo {
  id: string;
  question: string;
  type: string;
  options: string;
  correctAnswer: string;
  explanation: string;
}

export interface AdminContentInfo {
  id: string;
  level: DifficultyLevel;
  content: string;
  audioUrl: string | null;
  vocabularies: AdminVocabInfo[];
  quizzes: AdminQuizInfo[];
}

export interface AdminArticleJobInfo {
  jobId: string;
  status: string;
}

export interface AdminArticleDetailResponse {
  id: string;
  status: ArticleStatus;
  title: string;
  originalTitle: string;
  sourceName: string;
  sourceUrl: string;
  categoryId: string | null;
  categoryName: string | null;
  originalContent: string | null;
  createdAt: string;
  publishedAt: string;
  job: AdminArticleJobInfo | null;
  contents: AdminContentInfo[];
}
