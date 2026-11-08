CREATE TABLE api_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_prefix VARCHAR(12) NOT NULL UNIQUE,
    key_hash VARCHAR(64) NOT NULL,
    owner_label VARCHAR(100) NOT NULL,
    owner_email VARCHAR(254),
    scopes VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_api_key_expires_at ON api_key(expires_at) WHERE revoked_at IS NULL;
