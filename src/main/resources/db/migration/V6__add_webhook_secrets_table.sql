-- Webhook secrets table for HMAC signing and verification
-- As specified in PRD Section 48 - Webhook Design / Security (HMAC Signing)
-- Referenced in GraphQL schema for webhook secret management

CREATE TABLE IF NOT EXISTS webhook_secrets (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    rotated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_webhook_secrets_provider FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_webhook_secrets_workspace ON webhook_secrets(workspace_id);
CREATE INDEX IF NOT EXISTS idx_webhook_secrets_provider ON webhook_secrets(provider_id);
CREATE INDEX IF NOT EXISTS idx_webhook_secrets_active ON webhook_secrets(is_active) WHERE is_active = true;

COMMENT ON TABLE webhook_secrets IS 'Stores HMAC signing secrets for webhook authentication';
COMMENT ON COLUMN webhook_secrets.secret_key IS 'The HMAC-SHA256 shared secret for signing webhook payloads';
COMMENT ON COLUMN webhook_secrets.rotated_at IS 'Timestamp when the secret was last rotated for security';
COMMENT ON COLUMN webhook_secrets.expires_at IS 'Optional expiration date for automatic secret rotation';
COMMENT ON INDEX idx_webhook_secrets_active IS 'Fast lookup of active webhook secrets';