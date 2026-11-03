ALTER TABLE job_source ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_job_source_active ON job_source(source_name, active);
