CREATE INDEX idx_source_run_latest_success ON source_run(source_name, status, finished_at DESC);
