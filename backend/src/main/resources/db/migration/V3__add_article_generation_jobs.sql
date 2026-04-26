CREATE TABLE article_generation_jobs (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE article_generation_sub_jobs (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    level VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    CONSTRAINT fk_subjob_job FOREIGN KEY (job_id) REFERENCES article_generation_jobs(id),
    CONSTRAINT uk_subjob_job_level UNIQUE (job_id, level)
);
