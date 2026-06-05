export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';

export interface CursorPageResponse<T> {
  data: T[];
  nextCursor: string | null;
  hasNext: boolean;
}

export interface FeedArticleDto {
  id: string;
  title: string;
  categoryName: string;
  sourcePublisher: string;
  publishedAt: string;
  estimatedReadingTime: number;
  excerpt: string;
}

export interface ArticleDetailDto {
  id: string;
  title: string;
  sourceTitle: string;
  sourcePublisher: string;
  sourceUrl: string;
  publishedAt: string;
  sourcePublishedAt: string;
  sourceAccessedAt: string;
  categoryName: string;
  availableLevels: DifficultyLevel[];
  content: ArticleContentDto;
}

export interface ArticleContentDto {
  id: string;
  level: DifficultyLevel;
  content: string;
  audioUrl: string | null;
  vocabularies: VocabularyDto[];
  quizzes: QuizDto[];
}

export interface VocabularyDto {
  word: string;
  definition: string;
  exampleSentence: string;
}

export interface QuizChoiceDto {
  key: string;
  text: string;
  explanation: string | null;
}

export interface QuizOptionsDto {
  choices: QuizChoiceDto[] | null;
  explanations: Record<string, string> | null;
}

export interface QuizDto {
  id: string;
  type: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER' | 'SCRAMBLE';
  question: string;
  options: QuizOptionsDto;
  correctAnswer?: string;
  explanation?: string;
}

export interface QuizAttemptRequest {
  choiceId?: string;
  answerText?: string;
  answerList?: string[];
}

export interface QuizAttemptResponse {
  isCorrect: boolean;
  correctAnswer: string | string[];
  explanation: string;
}

export interface ApiErrorBody {
  error: string;
  message: string;
}
