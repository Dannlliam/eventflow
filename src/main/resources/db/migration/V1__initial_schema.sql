-- EventFlow Database Schema - Initial Migration
-- Creates the core tables for the EventFlow platform.

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_partman";

-- ============================================================
-- SCHEMA: eventflow
-- ============================================================
CREATE SCHEMA IF NOT EXISTS eventflow;

-- ============================================================
-- USERS & AUTHENTICATION
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS eventflow.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS eventflow.user_roles (
    user_id UUID NOT NULL REFERENCES eventflow.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES eventflow.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS eventflow.api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- ============================================================
-- TEMPLATES
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(slug)
);

CREATE TABLE IF NOT EXISTS eventflow.template_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL REFERENCES eventflow.templates(id) ON DELETE CASCADE,
    template_slug VARCHAR(100) NOT NULL,
    version INT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject_template TEXT,
    body_template TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID REFERENCES eventflow.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version_entity BIGINT NOT NULL DEFAULT 0,
    UNIQUE(template_id, version)
);

-- ============================================================
-- PROVIDERS
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.providers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit INT NOT NULL DEFAULT 100,
    rate_limit_duration_seconds INT NOT NULL DEFAULT 60,
    credentials JSONB NOT NULL DEFAULT '{}',
    settings JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_providers_workspace_channel
    ON eventflow.providers(workspace_id, channel);

-- ============================================================
-- NOTIFICATIONS (Partitioned by month)
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.notifications (
    id UUID NOT NULL DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient JSONB NOT NULL,
    template_slug VARCHAR(100),
    payload JSONB NOT NULL DEFAULT '{}',
    metadata JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    provider_id UUID,
    idempotency_key VARCHAR(255),
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions
CREATE TABLE eventflow.notifications_2024_q1
    PARTITION OF eventflow.notifications
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE eventflow.notifications_2024_q2
    PARTITION OF eventflow.notifications
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE eventflow.notifications_2024_q3
    PARTITION OF eventflow.notifications
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE eventflow.notifications_2024_q4
    PARTITION OF eventflow.notifications
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

-- Notification indexes
CREATE INDEX IF NOT EXISTS idx_notifications_workspace_created
    ON eventflow.notifications(workspace_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_idempotency
    ON eventflow.notifications(workspace_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_gin
    ON eventflow.notifications USING GIN (recipient);

CREATE INDEX IF NOT EXISTS idx_notifications_status_retry
    ON eventflow.notifications(status, next_retry_at)
    WHERE status = 'RETRY_SCHEDULED';

-- ============================================================
-- NOTIFICATION EVENTS (Partitioned by month)
-- ============================================================
CREATE TABLE IF NOT EXISTS eventflow.notification_events (
    id UUID NOT NULL DEFAULT uuid_generate_v4(),
    notification_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    provider_response JSONB,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions
CREATE TABLE eventflow.notification_events_2024_q1
    PARTITION OF eventflow.notification_events
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE eventflow.notification_events_2024_q2
    PARTITION OF eventflow.notification_events
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE eventflow.notification_events_2024_q3
    PARTITION OF eventflow.notification_events
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE eventflow.notification_events_2024_q4
    PARTITION OF eventflow.notification_events
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

-- Notification events indexes
CREATE INDEX IF NOT EXISTS idx_events_notification_id
    ON eventflow.notification_events(notification_id);

CREATE INDEX IF NOT EXISTS idx_events_created_brin
    ON eventflow.notification_events USING BRIN (created_at);

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

-- ============================================================
-- ANALYTICS MATERIALIZED VIEW
-- ============================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS eventflow.daily_delivery_stats AS
SELECT
    n.workspace_id,
    DATE(n.created_at) AS date,
    n.channel,
    n.provider_id,
    COUNT(*) AS total_sent,
    COUNT(*) FILTER (WHERE n.status = 'DELIVERED') AS total_delivered,
    COUNT(*) FILTER (WHERE n.status = 'FAILED') AS total_failed,
    COUNT(*) FILTER (WHERE n.status = 'DLQ') AS total_dlq
FROM eventflow.notifications n
GROUP BY n.workspace_id, DATE(n.created_at), n.channel, n.provider_id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_stats_unique
    ON eventflow.daily_delivery_stats(workspace_id, date, channel, provider_id);

-- ============================================================
-- SEED DATA: Default Roles
-- ============================================================
INSERT INTO eventflow.roles (id, name, description) VALUES
    (uuid_generate_v4(), 'WORKSPACE_ADMIN', 'Full access to workspace configuration and management'),
    (uuid_generate_v4(), 'DEVELOPER', 'Can manage templates, view notifications, trigger DLQ replays'),
    (uuid_generate_v4(), 'ANALYST', 'Read-only access to notifications, analytics, and audit logs')
ON CONFLICT (name) DO NOTHING;