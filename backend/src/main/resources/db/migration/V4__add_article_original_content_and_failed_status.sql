ALTER TABLE articles ADD COLUMN original_content TEXT;
ALTER TABLE articles ADD CONSTRAINT uk_articles_source_url UNIQUE (source_url);
