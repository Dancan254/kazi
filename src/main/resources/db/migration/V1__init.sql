CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    company VARCHAR(200) NOT NULL,
    location VARCHAR(300),
    remote BOOLEAN DEFAULT FALSE,
    salary_min INTEGER,
    salary_max INTEGER,
    currency VARCHAR(3),
    description TEXT,
    description_vector VECTOR(1536),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE job_source (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    source_name VARCHAR(50) NOT NULL,
    source_url TEXT NOT NULL,
    source_job_id VARCHAR(200),
    raw_data JSONB,
    first_seen_at TIMESTAMPTZ DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(job_id, source_name)
);

CREATE TABLE source_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name VARCHAR(50) NOT NULL,
    started_at TIMESTAMPTZ DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    status VARCHAR(20) DEFAULT 'running',
    jobs_fetched INTEGER DEFAULT 0,
    jobs_inserted INTEGER DEFAULT 0,
    error_message TEXT
);

CREATE TABLE search_query (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query_text TEXT NOT NULL,
    filters JSONB,
    result_count INTEGER NOT NULL,
    latency_ms INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_job_company ON job(company);
CREATE INDEX idx_job_status ON job(status);
CREATE INDEX idx_job_remote ON job(remote);
CREATE INDEX idx_job_source_name ON job_source(source_name);
