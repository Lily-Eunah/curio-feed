ALTER TABLE article_contents
    ADD COLUMN IF NOT EXISTS audio_data BYTEA;
