-- EventFlow Database Schema - V2 Migration
-- Adds audit logging, analytics improvements, and missing indexes.

-- ============================================================
-- AUDIT LOGS
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES eventflow.users(id),
    workspace_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    changes_json JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created
    ON eventflow.audit_logs(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action_created
    ON eventflow.audit_logs(action, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_workspace_created
    ON eventflow.audit_logs(workspace_id, created_at DESC);

-- ============================================================
-- USER DEVICES (for push notification token management)
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.user_devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    device_token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL, -- 'ios', 'android', 'web'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(device_token)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_user
    ON eventflow.user_devices(user_id, workspace_id);

CREATE INDEX IF NOT EXISTS idx_user_devices_active
    ON eventflow.user_devices(device_token)
    WHERE is_active = true;

-- ============================================================
-- SUPPRESSION LISTS (for compliance: opt-outs, bounces)
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.suppression_list (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(512) NOT NULL,
    reason VARCHAR(50) NOT NULL, -- 'BOUNCE', 'SPAM', 'UNSUBSCRIBE', 'COMPLAINT', 'INVALID'
    source VARCHAR(50), -- 'PROVIDER_CALLBACK', 'API', 'ADMIN'
    suppressed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(workspace_id, channel, recipient)
);

CREATE INDEX IF NOT EXISTS idx_suppression_lookup
    ON eventflow.suppression_list(workspace_id, channel, recipient);

-- ============================================================
-- IMPROVED ANALYTICS MATERIALIZED VIEW
-- ============================================================
-- Drop and recreate the materialized view with additional columns
DROP MATERIALIZED VIEW IF EXISTS eventflow.daily_delivery_stats;

CREATE MATERIALIZED VIEW eventflow.daily_delivery_stats AS
SELECT
    n.workspace_id,
    DATE(n.created_at) AS date,
    n.channel,
    n.provider_id,
    COUNT(*) AS total_sent,
    COUNT(*) FILTER (WHERE n.status = 'DELIVERED') AS total_delivered,
    COUNT(*) FILTER (WHERE n.status = 'FAILED') AS total_failed,
    COUNT(*) FILTER (WHERE n.status = 'DLQ') AS total_dlq,
    COUNT(*) FILTER (WHERE n.status = 'RETRY_SCHEDULED') AS total_retry_scheduled,
    COUNT(*) FILTER (WHERE n.status = 'SUPPRESSED') AS total_suppressed,
    AVG(EXTRACT(EPOCH FROM (n.delivered_at - n.created_at))) FILTER (WHERE n.delivered_at IS NOT NULL) AS avg_delivery_seconds
FROM eventflow.notifications n
GROUP BY n.workspace_id, DATE(n.created_at), n.channel, n.provider_id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_stats_unique
    ON eventflow.daily_delivery_stats(workspace_id, date, channel, provider_id);

-- ============================================================
-- ADDITIONAL INDEXES FOR PERFORMANCE
-- ============================================================
-- Index for notification event queries by type
CREATE INDEX IF NOT EXISTS idx_events_type_created
    ON eventflow.notification_events(event_type, created_at DESC);

-- Index for provider lookup by workspace and type
CREATE INDEX IF NOT EXISTS idx_providers_workspace_type
    ON eventflow.providers(workspace_id, provider_type);

-- Index for template version lookups
CREATE INDEX IF NOT EXISTS idx_template_versions_slug_active
    ON eventflow.template_versions(template_slug, is_active)
    WHERE is_active = true;

-- ============================================================
-- SEED DATA: Additional roles if not already present
-- ============================================================
INSERT INTO eventflow.roles (id, name, description) VALUES
    (uuid_generate_v4(), 'AUDITOR', 'Read-only access to audit logs for compliance auditing')
ON CONFLICT (name) DO NOTHING;