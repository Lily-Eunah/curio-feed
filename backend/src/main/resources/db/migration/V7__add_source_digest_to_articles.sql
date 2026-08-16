-- V7: Persist the SOURCE_DIGEST output on the article.
--
-- The digest is derived only from original_title + original_content, so it is identical for
-- every difficulty level. Storing it lets a resumed sub-job restore the digest instead of
-- re-generating it, and lets the second and third levels reuse it, cutting the LLM calls for
-- this step from three per article to one.

ALTER TABLE articles ADD COLUMN IF NOT EXISTS source_digest TEXT;
