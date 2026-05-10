CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE articles (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    original_title VARCHAR(500) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    source_url TEXT NOT NULL,
    original_published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL REFERENCES categories (id),
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(255) NOT NULL,
    original_content TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_articles_slug UNIQUE (slug),
    CONSTRAINT uk_articles_source_url UNIQUE (source_url)
);

-- Composite Indexes (Optimized for Pagination & Sorting)
CREATE INDEX idx_articles_status_published_at 
ON articles (status, published_at DESC, id DESC);

CREATE INDEX idx_articles_category_status_published_at 
ON articles (category_id, status, published_at DESC, id DESC);

CREATE TABLE article_contents (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    level VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    audio_url VARCHAR(255),
    article_id UUID NOT NULL REFERENCES articles (id),
    UNIQUE (article_id, level)
);

CREATE TABLE vocabularies (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    word VARCHAR(255) NOT NULL,
    definition TEXT NOT NULL,
    example_sentence TEXT,
    article_content_id UUID NOT NULL REFERENCES article_contents (id)
);

CREATE INDEX idx_vocab_article_content ON vocabularies (article_content_id);

CREATE TABLE quizzes (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    question TEXT NOT NULL,
    type VARCHAR(255) NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(255) NOT NULL,
    explanation TEXT,
    article_content_id UUID NOT NULL REFERENCES article_contents (id)
);

CREATE INDEX idx_quiz_article_content ON quizzes (article_content_id);

-- Generation Pipeline Tables
CREATE TABLE article_generation_jobs (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES articles(id),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE article_generation_sub_jobs (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES article_generation_jobs(id),
    level VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_subjob_job_level UNIQUE (job_id, level)
);

CREATE TABLE article_generation_step_jobs (
    id                UUID PRIMARY KEY,
    sub_job_id        UUID NOT NULL REFERENCES article_generation_sub_jobs(id),
    step_type         VARCHAR(20)  NOT NULL,  -- CONTENT | VOCABULARY | QUIZ
    status            VARCHAR(20)  NOT NULL,  -- PENDING | PROCESSING | COMPLETED | FAILED
    attempt_count     INT          NOT NULL DEFAULT 0,
    started_at        TIMESTAMP WITH TIME ZONE,
    completed_at      TIMESTAMP WITH TIME ZONE,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    validation_status VARCHAR(20),            -- PASS | FAIL
    validation_errors TEXT,
    error_message     TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_step_job_sub_job_step UNIQUE (sub_job_id, step_type)
);

CREATE INDEX idx_step_jobs_status ON article_generation_step_jobs(status);
CREATE INDEX idx_step_jobs_sub_job_id ON article_generation_step_jobs(sub_job_id);
