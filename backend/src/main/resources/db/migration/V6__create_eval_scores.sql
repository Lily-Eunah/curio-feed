CREATE TABLE eval_scores (
    id UUID PRIMARY KEY,
    article_content_id UUID NOT NULL REFERENCES article_contents(id) ON DELETE CASCADE,
    evaluator_model VARCHAR(100) NOT NULL,
    factual_accuracy DOUBLE PRECISION NOT NULL,
    level_appropriateness DOUBLE PRECISION NOT NULL,
    engagement DOUBLE PRECISION NOT NULL,
    safety DOUBLE PRECISION NOT NULL,
    overall DOUBLE PRECISION NOT NULL,
    raw_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_eval_scores_article_content_id ON eval_scores(article_content_id);
