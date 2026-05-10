-- Tracks per-step status for the 3-step generation pipeline:
-- CONTENT → VOCABULARY → QUIZ (each is a separate LLM call).
-- One row per (sub_job_id, step_type). Downstream steps are
-- created/unlocked when upstream completes.
CREATE TABLE article_generation_step_jobs (
    id                UUID PRIMARY KEY,
    sub_job_id        UUID NOT NULL,
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

    CONSTRAINT fk_step_job_sub_job FOREIGN KEY (sub_job_id)
        REFERENCES article_generation_sub_jobs(id),
    CONSTRAINT uk_step_job_sub_job_step UNIQUE (sub_job_id, step_type)
);

CREATE INDEX idx_step_jobs_status ON article_generation_step_jobs(status);
CREATE INDEX idx_step_jobs_sub_job_id ON article_generation_step_jobs(sub_job_id);
