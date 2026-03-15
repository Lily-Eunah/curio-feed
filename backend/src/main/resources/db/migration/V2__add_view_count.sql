-- Add view count column to articles table
ALTER TABLE articles ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
