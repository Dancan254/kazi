ALTER TABLE job_source DROP CONSTRAINT job_source_job_id_source_name_key;

ALTER TABLE job_source ALTER COLUMN source_job_id TYPE TEXT;

CREATE UNIQUE INDEX idx_job_source_source_posting
    ON job_source(source_name, source_job_id);
