CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE job_embedding (
    job_id UUID PRIMARY KEY REFERENCES job(id) ON DELETE CASCADE,
    model TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    embedding vector(768) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_job_embedding_cosine ON job_embedding USING hnsw (embedding vector_cosine_ops);
