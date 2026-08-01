-- Rate limiting tables for provider dispatch control
-- As specified in PRD Section 35 - Outbound Provider Rate Limiting

CREATE TABLE IF NOT EXISTS rate_limit_configs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    max_requests_per_minute INT NOT NULL DEFAULT 60,
    max_burst_size INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rate_limit_provider FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rate_limit_buckets (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    bucket_key VARCHAR(255) NOT NULL,
    tokens FLOAT NOT NULL DEFAULT 0,
    last_refill_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rate_limit_bucket_provider FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_workspace ON rate_limit_configs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_provider ON rate_limit_configs(provider_id);
CREATE INDEX IF NOT EXISTS idx_rate_limit_buckets_provider ON rate_limit_buckets(provider_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rate_limit_buckets_key ON rate_limit_buckets(provider_id, bucket_key);

COMMENT ON TABLE rate_limit_configs IS 'Configuration for provider rate limits';
COMMENT ON TABLE rate_limit_buckets IS 'Token bucket state for rate limiting enforcement';
COMMENT ON INDEX idx_rate_limit_configs_workspace IS 'Optimizes workspace-scoped rate limit queries';
COMMENT ON INDEX idx_rate_limit_buckets_key IS 'Ensures one bucket per provider per time window';