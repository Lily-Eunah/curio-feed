import { memo } from 'react';
import { COLORS } from '../../../theme';
import type { ArticleQuiz, ArticleQuizProgress, MCQResult, ShortAnswerResult } from '../../../types';
import MCQ from './MCQ';
import ShortAnswer from './ShortAnswer';

interface Props {
  articleId: string;
  quiz: ArticleQuiz;
  progress: ArticleQuizProgress;
  onAnswer: (articleId: string, qKey: 'q1' | 'q2' | 'q3', answer: MCQResult | ShortAnswerResult) => void;
}

export default memo(function QuizSection({ articleId, quiz, progress, onAnswer }: Props) {
  return (
    <div>
      <div
        style={{
          fontSize: 13,
          fontWeight: 700,
          color: COLORS.textTer,
          letterSpacing: '0.07em',
          textTransform: 'uppercase',
          marginBottom: 16,
        }}
      >
        Quick Quiz
      </div>
      <MCQ
        question={quiz.q1.question}
        options={quiz.q1.options}
        correct={quiz.q1.correct}
        explanation={quiz.q1.explanation}
        savedProgress={progress.q1}
        onAnswer={result => onAnswer(articleId, 'q1', result)}
      />
      <MCQ
        question={quiz.q2.question}
        options={quiz.q2.options}
        correct={quiz.q2.correct}
        explanation={quiz.q2.explanation}
        savedProgress={progress.q2}
        onAnswer={result => onAnswer(articleId, 'q2', result)}
      />
      <ShortAnswer
        question={quiz.q3.question}
        modelAnswer={quiz.q3.modelAnswer}
        savedProgress={progress.q3}
        onAnswer={result => onAnswer(articleId, 'q3', result)}
      />
    </div>
  );
});
