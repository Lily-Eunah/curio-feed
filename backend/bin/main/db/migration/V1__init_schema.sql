CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL,
    sort_order INT NOT NULL
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
    slug VARCHAR(255) NOT NULL UNIQUE,
    category_id UUID NOT NULL REFERENCES categories (id),
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(255) NOT NULL,
    thumbnail_url VARCHAR(255) NOT NULL
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
