ALTER TABLE article_generation_step_jobs
    ADD COLUMN prompt_version VARCHAR(50),
    ADD COLUMN model_name VARCHAR(100),
    ADD COLUMN quality_score DOUBLE PRECISION;
