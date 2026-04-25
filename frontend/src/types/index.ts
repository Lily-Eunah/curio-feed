export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';
export type Screen = 'feed' | 'article' | 'saved';

export interface VocabEntry {
  word: string;
  definition: string;
  example: string;
}

export interface MCQQuestion {
  question: string;
  options: string[];
  correct: number;
  explanation: string;
}

export interface ShortAnswerQuestion {
  question: string;
  modelAnswer: string;
}

export interface ArticleQuiz {
  q1: MCQQuestion;
  q2: MCQQuestion;
  q3: ShortAnswerQuestion;
}

export interface Article {
  id: string;
  category: string;
  date: string;
  readTime: string;
  title: string;
  excerpt: string;
  vocabulary: VocabEntry[];
  body: string;
  quiz: ArticleQuiz;
}

export interface MCQResult {
  status: 'done';
  correct: boolean;
  userAnswer: number;
  attempts: number;
}

export interface ShortAnswerResult {
  status: 'done';
  selfEval: 'yes' | 'no';
  userText: string;
}

export interface ArticleQuizProgress {
  q1?: MCQResult;
  q2?: MCQResult;
  q3?: ShortAnswerResult;
}

export interface ContinueReadingState {
  articleId: string;
  scrollPosition: number;
  progress: number;
  updatedAt: number;
}

export interface AppState {
  onboarded: boolean;
  userLevel: DifficultyLevel;
  selectedCategory: string;
  savedIds: string[];
  readIds: string[];
  visitedIds: string[];
  continueReading: ContinueReadingState | null;
  quizProgress: Record<string, ArticleQuizProgress>;
}
